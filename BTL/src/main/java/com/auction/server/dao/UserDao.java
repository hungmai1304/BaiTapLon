package com.auction.server.dao;

import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class UserDao {

    private static UserDao instance;

    private UserDao() {
    }

    // Synchronized đảm bảo an toàn đa luồng trên Server
    public static synchronized UserDao getInstance() {
        if (instance == null) {
            instance = new UserDao();
        }
        return instance;
    }

    /**
     * Thêm người dùng phân hệ Bidder (hoặc role tùy biến như ADMIN_REQUEST) có mã hóa mật khẩu BCrypt
     */
    public boolean insertBidder(String email, String password, String name, String id, Timestamp timeCreated, double balance, String role, String status) {
        // Check trùng email trước khi insert
        if (getUserByEmail(email) != null) {
            System.err.println("[UserDao]Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        // CẬP NHẬT SQL: Thêm cột status vào lệnh INSERT
        String sql = "INSERT INTO users (id, email, password, name, time_created, role, balance, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Mã hóa mật khẩu trước khi lưu
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setString(6, role);    // Thiết lập role động
            pstmt.setDouble(7, balance); // Đưa số dư vào statement
            pstmt.setString(8, status);  // CẬP NHẬT: Thiết lập status động ("NORMAL")

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi insert với role " + role + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Thêm người dùng phân hệ Seller có mã hóa mật khẩu BCrypt
     * CẬP NHẬT: Thêm tham số status nhận trạng thái mặc định từ Handler
     */
    public boolean insertSeller(String email, String password, String name, String id, Timestamp timeCreated, String shopName, double balance, String status) {
        // Check trùng email trước khi insert
        if (getUserByEmail(email) != null) {
            System.err.println("[UserDao] Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        // CẬP NHẬT SQL: Thêm cột status vào lệnh INSERT
        String sql = "INSERT INTO users (id, email, password, name, time_created, role, shop_name, balance, status) VALUES (?, ?, ?, ?, ?, 'SELLER', ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Mã hóa mật khẩu trước khi lưu
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setString(6, shopName);
            pstmt.setDouble(7, balance); // Đưa số dư vào statement
            pstmt.setString(8, status);  // CẬP NHẬT: Thiết lập status động ("NORMAL")

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi insert Seller: " + e.getMessage());
            return false;
        }
    }

    public User getUserByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi getUserByEmail: " + e.getMessage());
        }
        return null;
    }

    /**
     * Xác thực tài khoản dựa trên BCrypt mật khẩu băm
     */
    /**
     * Xác thực tài khoản linh hoạt: Hỗ trợ cả mật khẩu đã băm (BCrypt) và mật khẩu thô
     */
    public User authenticate(String email, String password) {
        User user = getUserByEmail(email);
        if (user == null) {
            return null;
        }


        if ("BANNED".equalsIgnoreCase(user.getStatus())) {
            System.err.println("[UserDao] Từ chối xác thực: Tài khoản [" + email + "] đang bị khóa (BANNED)!");
            return null;
        }

        // Trường hợp 1: Kiểm tra theo chuẩn mật khẩu đã mã hóa BCrypt (Bản 2)
        try {
            if (BCrypt.checkpw(password, user.getPassword())) {
                return user;
            }
        } catch (IllegalArgumentException e) {
            // Nhảy vào đây nếu mật khẩu trong DB là chuỗi thô (không phải định dạng hash của BCrypt)
            // Ta sẽ bỏ qua lỗi này để xuống kiểm tra trường hợp 2
        }

        // Trường hợp 2: Kiểm tra so sánh chuỗi thô trực tiếp (Bản 1 - dành cho dữ liệu cũ)
        if (password.equals(user.getPassword())) {
            return user;
        }
        return null;
    }

    // 3. CẬP NHẬT: Lấy dữ liệu balance từ DB gán ngược lại cho Object User
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user;
        String role = rs.getString("role");

        if ("SELLER".equalsIgnoreCase(role)) {
            Seller seller = new Seller();
            seller.setShopName(rs.getString("shop_name"));
            user = seller;
        } else if ("BIDDER".equalsIgnoreCase(role)) {
            user = new Bidder();
        } else if ("ADMIN".equalsIgnoreCase(role)) {
            user = new Admin();
        } else {
            user = new User();
        }

        user.setRole(role);
        user.setId(rs.getString("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setUsername(rs.getString("name"));
        user.setShopName(rs.getString("shop_name"));

        // --- THÊM DÒNG NÀY ĐỂ ĐỌC SỐ DƯ ---
        user.setRole(role);
        user.setBalance(rs.getDouble("balance"));

        // CẬP NHẬT: Đọc cột dữ liệu status từ DB đẩy vào Object Java mẫu
        user.setStatus(rs.getString("status"));

        // 3. Sử dụng getTimestamp để JDBC tự động parse sang LocalDateTime an toàn hơn
        // 3. Đọc dữ liệu AVATAR (Từ bytea trong DB sang Base64 cho Client hiển thị)
        byte[] avatarBytes = rs.getBytes("avatar");
        if (avatarBytes != null) {
            String base64Avatar = java.util.Base64.getEncoder().encodeToString(avatarBytes);
            user.setAvatar(base64Avatar);
        }

        // FIX DỨT ĐIỂM Ở ĐÂY: Dùng getTimestamp để JDBC tự parse LocalDateTime
        Timestamp ts = rs.getTimestamp("time_created");
        if (ts != null) {
            user.setTimeCreated(ts.toLocalDateTime());
        }

        return user;
    }

    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi findById: " + e.getMessage());
        }
        return null;
    }

    /**
     * Cập nhật số dư tài khoản (Nạp tiền vào tài khoản)
     *
     * @param email  Email của người dùng cần nạp tiền
     * @param amount Số tiền cần nạp (phải > 0)
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean depositMoney(String email, double amount) {
        if (amount <= 0) {
            System.err.println("❌ Lỗi: Số tiền nạp phải lớn hơn 0!");
            return false;
        }

        // Câu lệnh SQL cộng dồn số tiền hiện tại với số tiền nạp mới
        String sql = "UPDATE users SET balance = balance + ? WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setString(2, email);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi thực hiện nạp tiền cho: " + email + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật số dư tài khoản (Rút tiền khỏi tài khoản)
     *
     * @param email  Email của người dùng cần rút tiền
     * @param amount Số tiền cần rút (phải > 0 và phải <= số dư hiện tại)
     * @return true nếu rút thành công, false nếu thất bại hoặc không đủ tiền
     */
    public boolean withdrawMoney(String email, double amount) {
        if (amount <= 0) {
            System.err.println("❌ Lỗi: Số tiền rút phải lớn hơn 0!");
            return false;
        }

        // Bước 1: Kiểm tra số dư tài khoản hiện tại trước khi cho phép rút
        User user = getUserByEmail(email);
        if (user == null) {
            System.err.println("❌ Lỗi: Không tìm thấy người dùng!");
            return false;
        }

        if (user.getBalance() < amount) {
            System.err.println("❌ Lỗi: Tài khoản " + email + " không đủ số dư để rút (" + user.getBalance() + " < " + amount + ")");
            return false; // Trả về false luôn để Handler biết là do không đủ tiền
        }

        // Bước 2: Thực hiện trừ tiền trong Database
        String sql = "UPDATE users SET balance = balance - ? WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setString(2, email);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi thực hiện rút tiền cho: " + email + " - " + e.getMessage());
            return false;
        }

    }

    /**
     * Lấy riêng số dư hiện tại của tài khoản dựa trên Email
     *
     * @param email Email của người dùng cần kiểm tra
     * @return Số dư tài khoản (trả về 0.0 nếu không tìm thấy user hoặc lỗi)
     */
    public double getBalanceByEmail(String email) {
        String sql = "SELECT balance FROM users WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("balance");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi getBalanceByEmail: " + e.getMessage());
        }
        return 0.0;
    }

    /**
     * Lấy danh sách người dùng dựa theo Role (Ví dụ: "ADMIN_REQUEST")
     *
     * @param role Tên quyền cần tìm kiếm trong DB
     * @return Danh sách đối tượng User tìm thấy
     */
    public List<User> getUsersByRole(String role) {
        List<User> userList = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE UPPER(role) = ? ORDER BY time_created DESC";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, role.toUpperCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userList.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi trong hàm getUsersByRole: " + e.getMessage());
        }
        return userList;
    }

    /**
     * Cập nhật Role (Quyền) của người dùng dựa trên ID
     *
     * @param id      ID của người dùng cần cập nhật
     * @param newRole Quyền mới muốn gán (Ví dụ: "ADMIN")
     * @return true nếu cập nhật thành công, false nếu thất bại
     */
    public boolean updateUserRole(String id, String newRole) {
        String sql = "UPDATE users SET role = ? WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newRole);
            pstmt.setString(2, id);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi khi cập nhật role cho user ID " + id + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật Avatar cho người dùng
     *
     * @param email        Email của người dùng
     * @param avatarBase64 Chuỗi ảnh Base64
     * @return true nếu cập nhật thành công
     */
    public boolean updateUserAvatar(String email, String avatarBase64) {
        String sql = "UPDATE users SET avatar = ? WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Chuyển Base64 thành byte array để lưu vào cột kiểu bytea
            byte[] avatarBytes = java.util.Base64.getDecoder().decode(avatarBase64);
            pstmt.setBytes(1, avatarBytes);
            pstmt.setString(2, email);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi updateUserAvatar cho " + email + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Khấu trừ số dư tài khoản tự động (Khi người dùng thắng / chốt đơn đấu giá thành công)
     * Có tích hợp kiểm tra bảo mật kép ngay tại tầng câu lệnh SQL nhằm tránh tranh chấp tài nguyên (Race Condition)
     * @param email Email của người dùng bị trừ tiền
     * @param amountToDeduct Số tiền cần trừ
     * @return true nếu trừ tiền thành công
     */
    /**
     * Hàm trừ tiền khi chốt đơn đấu giá thành công
     */
    public boolean deductBalance(String email, double amountToDeduct) {
        // Câu lệnh SQL: Trừ tiền với điều kiện số dư hiện tại phải lớn hơn hoặc bằng số tiền cần trừ (Bảo mật kép dưới DB)
        String sql = "UPDATE users SET balance = balance - ? WHERE email = ? AND balance >= ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amountToDeduct);
            pstmt.setString(2, email);
            pstmt.setDouble(3, amountToDeduct);

            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;

        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi khi trừ tiền của user " + email + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * VIẾT THÊM: Cập nhật trạng thái (status) cho người dùng tìm theo Email.
     * Thường dùng khi Admin gửi yêu cầu BAN / LOCK hoặc UNLOCK tài khoản.
     *
     * @param email     Email của người dùng cần thay đổi trạng thái
     * @param newStatus Trạng thái mới (Ví dụ: "NORMAL", "BANNED")
     * @return true nếu cập nhật thành công xuống cơ sở dữ liệu, ngược lại là false
     */
    public boolean updateUserStatus(String email, String newStatus) {
        if (email == null || email.trim().isEmpty() || newStatus == null) return false;

        String sql = "UPDATE users SET status = ? WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setString(2, email);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[UserDao] Cập nhật status tài khoản [" + email + "] thành công sang: " + newStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi trong hàm updateUserStatus khi cập nhật email " + email + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * VIẾT THÊM: Lấy danh sách người dùng dựa theo Trạng thái (Ví dụ: "BANNED", "NORMAL")
     * Thường dùng khi Admin muốn hiển thị danh sách các tài khoản đang bị khóa.
     * * @param status Trạng thái cần tìm kiếm (Ví dụ: "BANNED")
     *
     * @return Danh sách các Object User (hoặc lớp con tương ứng) thỏa mãn điều kiện
     */

    public List<User> getUsersByStatus(String status) {
        List<User> userList = new ArrayList<>();
        if (status == null || status.trim().isEmpty()) {
            return userList;
        }

        // Truy vấn sắp xếp theo thời gian tạo mới nhất lên đầu
        String sql = "SELECT * FROM users WHERE UPPER(status) = ? ORDER BY time_created DESC";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.toUpperCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Tận dụng hàm mapResultSetToUser có sẵn để tự động nhận diện Admin, Seller, Bidder
                    userList.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi trong hàm getUsersByStatus khi tìm trạng thái " + status + ": " + e.getMessage());
        }
        return userList;
    }
    /**
     * Đếm tổng số sản phẩm mà một người dùng (Shop) đang sở hữu trong hệ thống
     * @param userId ID của người dùng cần đếm sản phẩm
     * @return Số lượng sản phẩm (trả về 0 nếu không có hoặc lỗi)
     */
    public int countProductsByOwnerId(String userId) {
        if (userId == null || userId.trim().isEmpty()) return 0;

        String sql = "SELECT COUNT(*) AS total FROM products WHERE owner_id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi trong hàm countProductsByOwnerId cho User ID " + userId + ": " + e.getMessage());
        }
        return 0;
    }

}