package com.auction.server.dao;

import com.auction.server.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class AuctionDao {

    private static AuctionDao instance;

    private AuctionDao() {}

    public static synchronized AuctionDao getInstance() {
        if (instance == null) {
            instance = new AuctionDao();
        }
        return instance;
    }

    public boolean saveCompletedAuction(int auctionId, String productId, String winnerEmail, double finalPrice) {
        // Lưu thông tin phiên với trạng thái COMPLETED
        String sql = "INSERT INTO auctions (id, product_id, winner_email, final_price, status) VALUES (?, ?, ?, ?, 'COMPLETED')";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, auctionId);
            pstmt.setString(2, productId);
            // Nếu không có người thắng thì lưu là "No Winner"
            pstmt.setString(3, winnerEmail != null ? winnerEmail : "No Winner");
            pstmt.setDouble(4, finalPrice);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AuctionDao] Lỗi khi lưu lịch sử đấu giá: " + e.getMessage());
            return false;
        }
    }
}