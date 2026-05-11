package com.auction.server.dao;
// file này sửa thoải mái
import com.auction.common.model.product.Product;
import com.auction.server.db.Db;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

public class ProductDao {

    private static ProductDao instance;

    // Constructor private để ngăn chặn tạo mới từ bên ngoài
    private ProductDao() {}

    // Hàm getInstance để lấy đối tượng duy nhất
    public static synchronized ProductDao getInstance() {
        if (instance == null) {
            instance = new ProductDao();
        }
        return instance;
    }

    /**
     * Hàm lưu sản phẩm vào Database
     */
    public boolean saveProduct(Product product) {
        String sql = "INSERT INTO products (id, name, category, description, start_price, current_price, step_price, status, owner_id, time_created) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getId());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setString(4, product.getDescription());
            pstmt.setDouble(5, product.getStartPrice());
            pstmt.setDouble(6, product.getCurrentPrice());
            pstmt.setDouble(7, product.getStepPrice());
            pstmt.setString(8, product.getStatus().toString());

            // Lưu ID của owner (User)
            pstmt.setString(9, product.getOwner().getId());

            // Chuyển LocalDateTime sang Timestamp để lưu vào SQL
            pstmt.setTimestamp(10, Timestamp.valueOf(product.getTimeCreated()));

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu sản phẩm: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}