package com.auction.server.dao;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDao {

    private static AuctionDao instance;

    private AuctionDao() {}

    public static synchronized AuctionDao getInstance() {
        if (instance == null) {
            instance = new AuctionDao();
        }
        return instance;
    }

    public List<Auction> getWonAuctionsByEmail(String email) {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.*, p.name as product_name, p.image_path, p.end_time, u.name as seller_name " +
                     "FROM auctions a " +
                     "JOIN products p ON a.product_id = p.id " +
                     "JOIN users u ON p.owner_id = u.id " +
                     "WHERE a.winner_email = ? AND a.status = 'COMPLETED'";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Auction auction = new Auction();
                    auction.setId(rs.getString("id"));
                    auction.setCurrentPrice(rs.getDouble("final_price"));
                    auction.setStatus(rs.getString("status"));

                    Timestamp endTimeTs = rs.getTimestamp("end_time");
                    if (endTimeTs != null) {
                        auction.setEndTime(endTimeTs.toLocalDateTime());
                    }

                    Product product = new Product();
                    product.setId(rs.getString("product_id"));
                    product.setName(rs.getString("product_name"));
                    product.setImagePath(rs.getString("image_path"));
                    
                    User seller = new User();
                    seller.setUsername(rs.getString("seller_name"));
                    product.setOwner(seller);
                    
                    auction.setProduct(product);
                    
                    list.add(auction);
                }
            }
        } catch (SQLException e) {
            System.err.println("[AuctionDao] Lỗi khi lấy danh sách sản phẩm thắng: " + e.getMessage());
        }
        return list;
    }

    public boolean saveCompletedAuction(String auctionId, String productId, String winnerEmail, double finalPrice) {
        // Lưu thông tin phiên với trạng thái COMPLETED
        String sql = "INSERT INTO auctions (id, product_id, winner_email, final_price, status) VALUES (?, ?, ?, ?, 'COMPLETED')";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auctionId);
            pstmt.setString(2, productId);
            pstmt.setString(3, winnerEmail != null ? winnerEmail : "No Winner");
            pstmt.setDouble(4, finalPrice);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AuctionDao] Lỗi khi lưu lịch sử đấu giá: " + e.getMessage());
            return false;
        }
    }
}