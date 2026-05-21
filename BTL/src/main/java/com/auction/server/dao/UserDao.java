package com.auction.server.dao;

import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class UserDao {

    private static UserDao instance;

    private UserDao() {}

    // Synchronized đảm bảo an toàn đa luồng khi khởi tạo Singleton
    public static synchronized UserDao getInstance() {
        if (instance == null) {
            instance = new UserDao();
        }
        return instance;
    }

    /**
     * Thêm người dùng phân hệ Bidder - Tối ưu check trùng bằng EXISTS (Nhanh hơn lấy cả Object)
     */
    public boolean insertBidder(String email, String password, String name, String id, Timestamp timeCreated, double balance, String role, String status) {
        // TỐI ƯU: Chỉ kiểm tra sự tồn tại của Email bằng câu lệnh siêu nhẹ, không cần lôi cả bản ghi lên RAM
        if (isEmailExists(email)) {
            System.err.println("[UserDao] Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        String sql = "INSERT INTO users (id, email, password, name, time_created, role, balance, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Mã hóa mật khẩu BCrypt
            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setString(6, role);
            pstmt.setDouble(7, balance);
            pstmt.setString(8, status);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi insert với role " + role + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Thêm người dùng phân hệ Seller - Tối ưu check trùng bằng EXISTS
     */
    public boolean insertSeller(String email, String password, String name, String id, Timestamp timeCreated, String shopName, double balance, String status) {
        if (isEmailExists(email)) {
            System.err.println("[UserDao] Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        String sql = "INSERT INTO users (id, email, password, name, time_created, role, shop_name, balance, status) VALUES (?, ?, ?, ?, ?, 'SELLER', ?, ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            String hashedPassword = BCrypt.hashpw(password, BCrypt.gensalt());

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, hashedPassword);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setString(6, shopName);
            pstmt.setDouble(7, balance);
            pstmt.setString(8, status);

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
     * Xác thực tài khoản linh hoạt: Hỗ trợ cả mật khẩu băm và thô
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

        // Trường hợp 1: Kiểm tra theo chuẩn mật khẩu đã mã hóa BCrypt
        try {
            if (BCrypt.checkpw(password, user.getPassword())) {
                return user;
            }
        } catch (IllegalArgumentException e) {
            // Bỏ qua lỗi định dạng nếu DB chứa mật khẩu chuỗi thô (dữ liệu cũ)
        }

        // Trường hợp 2: So sánh chuỗi thô trực tiếp
        if (password.equals(user.getPassword())) {
            return user;
        }
        return null;
    }

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
        user.setBalance(rs.getDouble("balance"));
        user.setStatus(rs.getString("status"));

        byte[] avatarBytes = rs.getBytes("avatar");
        if (avatarBytes != null) {
            String base64Avatar = Base64.getEncoder().encodeToString(avatarBytes);
            user.setAvatar(base64Avatar);
        }

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
     */
    public boolean depositMoney(String email, double amount) {
        if (amount <= 0) {
            System.err.println("❌ Lỗi: Số tiền nạp phải lớn hơn 0!");
            return false;
        }

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
     * ĐÃ TỐI ƯU TUYỆT ĐỐI: Rút tiền khỏi tài khoản (Chỉ tốn 1 database hit và chống Race Condition)
     */
    public boolean withdrawMoney(String email, double amount) {
        if (amount <= 0) {
            System.err.println("❌ Lỗi: Số tiền rút phải lớn hơn 0!");
            return false;
        }

        // TỐI ƯU: Gộp điều kiện kiểm tra số dư trực tiếp vào câu UPDATE (Atomic Operation)
        // Loại bỏ hoàn toàn bước getUserByEmail cũ để giảm tải 50% thời gian kết nối mạng xuống DB
        String sql = "UPDATE users SET balance = balance - ? WHERE email = ? AND balance >= ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amount);
            pstmt.setString(2, email);
            pstmt.setDouble(3, amount);

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                return true;
            } else {
                // Nếu update thất bại (0 dòng bị ảnh hưởng), có thể do không đủ tiền hoặc tài khoản không tồn tại
                System.err.println("❌ Lỗi: Rút tiền thất bại cho [" + email + "]. Lý do: Không đủ số dư hoặc tài khoản không tồn tại.");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("❌ Lỗi khi thực hiện rút tiền cho: " + email + " - " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy riêng số dư hiện tại của tài khoản dựa trên Email
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
     * Lấy danh sách người dùng dựa theo Role
     */
    public List<User> getUsersByRole(String role) {
        List<User> userList = new ArrayList<>();
        // TỐI ƯU: Nếu cột 'role' trong DB đã viết hoa chuẩn hóa, hãy bỏ hàm UPPER() đi để DB ăn được INDEX của cột đó nhé!
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
     */
    public boolean updateUserAvatar(String email, String avatarBase64) {
        String sql = "UPDATE users SET avatar = ? WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            byte[] avatarBytes = Base64.getDecoder().decode(avatarBase64);
            pstmt.setBytes(1, avatarBytes);
            pstmt.setString(2, email);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi updateUserAvatar cho " + email + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Hàm trừ tiền khi chốt đơn đấu giá thành công (Atomic Safe)
     */
    public boolean deductBalance(String email, double amountToDeduct) {
        String sql = "UPDATE users SET balance = balance - ? WHERE email = ? AND balance >= ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, amountToDeduct);
            pstmt.setString(2, email);
            pstmt.setDouble(3, amountToDeduct);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi khi trừ tiền của user " + email + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Cập nhật trạng thái (status) cho người dùng tìm theo Email.
     */
    public boolean updateUserStatus(String email, String newStatus) {
        if (email == null || email.trim().isEmpty() || newStatus == null) return false;

        String sql = "UPDATE users SET status = ? WHERE email = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, newStatus);
            pstmt.setString(2, email);

            if (pstmt.executeUpdate() > 0) {
                System.out.println("[UserDao] Cập nhật status tài khoản [" + email + "] thành công sang: " + newStatus);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi trong hàm updateUserStatus khi cập nhật email " + email + ": " + e.getMessage());
        }
        return false;
    }

    /**
     * Lấy danh sách người dùng dựa theo Trạng thái
     */
    public List<User> getUsersByStatus(String status) {
        List<User> userList = new ArrayList<>();
        if (status == null || status.trim().isEmpty()) {
            return userList;
        }

        String sql = "SELECT * FROM users WHERE UPPER(status) = ? ORDER BY time_created DESC";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.toUpperCase());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    userList.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[UserDao] Lỗi trong hàm getUsersByStatus khi tìm trạng thái " + status + ": " + e.getMessage());
        }
        return userList;
    }

    /**
     * Đếm tổng số sản phẩm mà một người dùng (Shop) đang sở hữu
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

    /**
     * HÀM VIẾT THÊM (BẢO VỆ HIỆU NĂNG): Kiểm tra Email tồn tại bằng câu lệnh siêu nhẹ EXISTS
     */
    private boolean isEmailExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ? LIMIT 1";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next(); // Nếu có bản ghi trả về true, ngược lại false
            }
        } catch (SQLException e) {
            return false;
        }
    }
    public List<Map<String, Object>> getAllShopsWithProductCount() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        // Dùng LEFT JOIN để shop chưa có sản phẩm nào vẫn hiện ra với số lượng = 0
        String sql = "SELECT u.id, u.email, u.name, u.shop_name, u.status, COUNT(p.id) as product_count " +
                "FROM users u " +
                "LEFT JOIN products p ON u.id = p.owner_id " +
                "WHERE u.role = 'SELLER' " +
                "GROUP BY u.id, u.email, u.name, u.shop_name, u.status";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                Map<String, Object> shopInfo = new HashMap<>();
                shopInfo.put("id", rs.getString("id"));
                shopInfo.put("email", rs.getString("email"));
                shopInfo.put("name", rs.getString("name"));
                shopInfo.put("shopName", rs.getString("shop_name"));
                shopInfo.put("status", rs.getString("status"));
                shopInfo.put("productCount", rs.getInt("product_count"));

                list.add(shopInfo);
            }
        }
        return list;
    }
}