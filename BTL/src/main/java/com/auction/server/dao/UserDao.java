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

    // Synchronized để đảm bảo an toàn đa luồng trên Server
    public static synchronized UserDao getInstance() {
        if (instance == null) {
            instance = new UserDao();
        }
        return instance;
    }



    public boolean insertBidder(String email, String password, String name, String id, Timestamp timeCreated) {
        // 1. Check trùng email trước khi insert
        if (getUserByEmail(email) != null) {
            System.err.println("❌ Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        String sql = "INSERT INTO users (id, email, password, name, time_created, role) VALUES (?, ?, ?, ?, ?, 'BIDDER')";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("❌ Lỗi insert Bidder: " + e.getMessage());
            return false;
        }
    }

    public boolean insertSeller(String email, String password, String name, String id, Timestamp timeCreated, String shopName) {
        // 1. Check trùng email trước khi insert
        if (getUserByEmail(email) != null) {
            System.err.println("❌ Lỗi: Email " + email + " đã tồn tại!");
            return false;
        }

        String sql = "INSERT INTO users (id, email, password, name, time_created, role, shop_name) VALUES (?, ?, ?, ?, ?, 'SELLER', ?)";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setString(6, shopName);

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
        return null; // Trả về null nếu không tìm thấy
    }

// ... các hàm mapResultSetToUser phía dưới giữ nguyên ...

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



    // Hàm hỗ trợ map dữ liệu (Đã sửa lỗi parse thời gian)
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

        // FIX DỨT ĐIỂM Ở ĐÂY: Dùng getTimestamp để JDBC tự parse LocalDateTime
        Timestamp ts = rs.getTimestamp("time_created");
        if (ts != null) {
            user.setTimeCreated(ts.toLocalDateTime());
        }

        return user;
    }

    // Các hàm tìm kiếm khác (Dọn dẹp Try-with-resources)
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
}