package com.auction.server.dao;

import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private static UserDao instance;

    private UserDao() {}

    // Synchronized đảm bảo an toàn đa luồng trên Server
    public static synchronized UserDao getInstance() {
        if (instance == null) {
            instance = new UserDao();
        }
        return instance;
    }

    // 1. CẬP NHẬT: Thêm tham số double balance và đưa vào câu lệnh INSERT
    public boolean insertBidder(String email, String password, String name, String id, Timestamp timeCreated, double balance) {
        // Check trùng email trước khi insert
        if (getUserByEmail(email) != null) {
            System.err.println("❌ Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        // Thêm cột balance vào SQL INSERT
        String sql = "INSERT INTO users (id, email, password, name, time_created, role, balance) VALUES (?, ?, ?, ?, ?, 'BIDDER', ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setDouble(6, balance); // Đưa số dư vào statement

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi insert Bidder: " + e.getMessage());
            return false;
        }
    }

    // 2. CẬP NHẬT: Thêm tham số double balance và đưa vào câu lệnh INSERT
    public boolean insertSeller(String email, String password, String name, String id, Timestamp timeCreated, String shopName, double balance) {
        // Check trùng email trước khi insert
        if (getUserByEmail(email) != null) {
            System.err.println("❌ Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        // Thêm cột balance vào SQL INSERT
        String sql = "INSERT INTO users (id, email, password, name, time_created, role, shop_name, balance) VALUES (?, ?, ?, ?, ?, 'SELLER', ?, ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setString(6, shopName);
            pstmt.setDouble(7, balance); // Đưa số dư vào statement

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi insert Seller: " + e.getMessage());
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
            System.err.println("❌ Lỗi getUserByEmail: " + e.getMessage());
        }
        return null;
    }

    public User authenticate(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi authenticate: " + e.getMessage());
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
        } else {
            user = new User();
        }

        user.setId(rs.getString("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setUsername(rs.getString("name"));

        // --- THÊM DÒNG NÀY ĐỂ ĐỌC SỐ DƯ ---
        user.setBalance(rs.getDouble("balance"));

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
            System.err.println("Lỗi findById: " + e.getMessage());
        }
        return null;
    }
    /**
     * Cập nhật số dư tài khoản (Nạp tiền vào tài khoản)
     * @param email Email của người dùng cần nạp tiền
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
     * @param email Email của người dùng cần rút tiền
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
}