package com.auction.server.dao;

import com.auction.server.db.Db;
import com.auction.server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    private static UserDao instance;

    private UserDao() {}

    public static UserDao getInstance() {
        if (instance == null) {
            instance = new UserDao();
        }
        return instance;
    }


    public boolean insertBidder(String email, String password, String name, String id, Timestamp timeCreated) {
        String sql = "INSERT INTO users (id, email, password, name, time_created, role) VALUES (?, ?, ?, ?, ?, 'BIDDER')";
        try {Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insert Bidder: " + e.getMessage());
            return false;
        }
    }

    public boolean insertSeller(String email, String password, String name, String id, Timestamp timeCreated, String shopName) {
        String sql = "INSERT INTO users (id, email, password, name, time_created, role, shop_name) VALUES (?, ?, ?, ?, ?, 'SELLER', ?)";
        try {Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setString(1, id);
            pstmt.setString(2, email);
            pstmt.setString(3, password);
            pstmt.setString(4, name);
            pstmt.setTimestamp(5, timeCreated);
            pstmt.setString(6, shopName);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Lỗi insert Seller: " + e.getMessage());
            return false;
        }
    }

    //truy xuất user
    public User authenticate(String email, String password) {
        String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
        try {Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);

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

    public List<User> findByTimeCreated(Timestamp timeCreated) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE time_created = ?";
        try {Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);

            pstmt.setTimestamp(1, timeCreated);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi findByTimeCreated: " + e.getMessage());
        }
        return users;
    }


    public List<User> findByName(String name) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users WHERE name ILIKE ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, "%" + name + "%");
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi findByName: " + e.getMessage());
        }
        return users;
    }

    // Hàm phụ trợ(làm sạch code, tránh lặp)

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getString("id"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setName(rs.getString("name"));
        user.setTimeCreated(rs.getTimestamp("time_created"));
        user.setRole(rs.getString("role"));
        user.setShopName(rs.getString("shop_name"));
        return user;
    }
}