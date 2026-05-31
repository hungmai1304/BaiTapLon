package com.auction.server.dao;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDao {

    private static AuctionDao instance;

    private AuctionDao() {}

    // Singleton Thread-safe chuẩn chỉ bằng Lazy Initialization Double-Checked Locking nếu muốn tối ưu,
    // hoặc giữ nguyên synchronized nếu tần suất gọi getInstance không quá nghẽn.
    public static synchronized AuctionDao getInstance() {
        if (instance == null) {
            instance = new AuctionDao();
        }
        return instance;
    }

    /**
     * Lấy danh sách phiên đấu giá thắng theo Email.
     * Ném SQLException lên trên để tầng nghiệp vụ (Handler) biết rõ nếu DB gặp sự cố.
     */
    public List<Auction> getWonAuctionsByEmail(String email) throws SQLException {
        List<Auction> list = new ArrayList<>();
        String sql = "SELECT a.id, a.final_price, a.status, a.product_id, " +
                "p.name as product_name, p.image_path, p.end_time, u.name as seller_name " +
                "FROM auctions a " +
                "JOIN products p ON a.product_id = p.id " +
                "JOIN users u ON p.owner_id = u.id " +
                "WHERE a.winner_email = ? AND a.status = 'COMPLETED'";

        // Try-with-resources bảo vệ tài nguyên hệ thống gọn gàng
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
        } // Không nuốt ngoại lệ ở đây nữa, đẩy ra ngoài cho Handler xử lý bọc lỗi
        return list;
    }

    /**
     * Lưu thông tin khi phiên đấu giá kết thúc thành công (Hoặc ế hàng).
     */
    public boolean saveCompletedAuction(String auctionId, String productId, String winnerEmail, double finalPrice) {
        // Kiểm tra tính hợp lệ của dữ liệu đầu vào trước khi mở kết nối DB
        if (auctionId == null || auctionId.trim().isEmpty() || productId == null || productId.trim().isEmpty()) {
            System.err.println("[AuctionDao] Lỗi dữ liệu: id phiên hoặc id sản phẩm bị null/rỗng.");
            return false;
        }

        String sql = "INSERT INTO auctions (id, product_id, winner_email, final_price, status) VALUES (?, ?, ?, ?, 'COMPLETED')";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, auctionId);
            pstmt.setString(2, productId);

            // XỬ LÝ FIX LỖI: Nếu không có người thắng cuộc, lưu giá trị NULL thực sự vào DB thay vì chuỗi rác
            if (winnerEmail == null || winnerEmail.trim().isEmpty() || "No Winner".equalsIgnoreCase(winnerEmail.trim())) {
                pstmt.setNull(3, Types.VARCHAR);
            } else {
                pstmt.setString(3, winnerEmail.trim());
            }

            pstmt.setDouble(4, finalPrice);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("[AuctionDao] Lỗi SQL nghiêm trọng khi lưu lịch sử đấu giá: " + e.getMessage());
            return false;
        }
    }
}