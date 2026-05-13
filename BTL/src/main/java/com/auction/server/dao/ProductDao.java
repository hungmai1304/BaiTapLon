package com.auction.server.dao;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    private static ProductDao instance;
    private ProductDao() {}

    // Hàm getInstance để lấy đối tượng duy nhất
    public static synchronized ProductDao getInstance() {
        if (instance == null) {
            instance = new ProductDao();
        }
        return instance;
    }

    /**
     * 1. Hàm lưu sản phẩm vào Database
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
            pstmt.setString(8, product.getStatus().name());
            pstmt.setString(9, product.getOwner().getId());
            pstmt.setTimestamp(10, Timestamp.valueOf(product.getTimeCreated()));

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu sản phẩm: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 2. Hàm cập nhật sản phẩm
     */
    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name=?, category=?, description=?, start_price=?, current_price=?, step_price=?, status=? WHERE id=?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getCategory());
            pstmt.setString(3, product.getDescription());
            pstmt.setDouble(4, product.getStartPrice());
            pstmt.setDouble(5, product.getCurrentPrice());
            pstmt.setDouble(6, product.getStepPrice());
            pstmt.setString(7, product.getStatus().name());

            // Điều kiện WHERE id = ?
            pstmt.setString(8, product.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi cập nhật sản phẩm: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 3. Hàm lấy danh sách sản phẩm để Broadcast
     * Lấy ra các sản phẩm đang ở trạng thái AVAILABLE
     */
    public List<Product> getAvailableProducts() {
        List<Product> products = new ArrayList<>();
        // Sắp xếp theo thời gian tạo(mới nhất lên đầu)
        String sql = "SELECT * FROM products WHERE status = ? ORDER BY time_created DESC";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Giả sử lấy các sản phẩm đang AVAILABLE để hiển thị trên sàn
            pstmt.setString(1, ProductStatus.AVAILABLE.name());

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product();

                    // Set thuộc tính kế thừa từ Item
                    product.setId(rs.getString("id"));
                    product.setName(rs.getString("name"));

                    // Set thuộc tính của Product
                    product.setCategory(rs.getString("category"));
                    product.setDescription(rs.getString("description"));
                    product.setStartPrice(rs.getDouble("start_price"));
                    product.setCurrentPrice(rs.getDouble("current_price"));
                    product.setStepPrice(rs.getDouble("step_price"));

                    // Map Status
                    String statusStr = rs.getString("status");
                    if (statusStr != null) {
                        product.setStatus(ProductStatus.valueOf(statusStr));
                    }

                    // Map Owner (Gắn ID cho User)
                    User owner = new User();
                    // Giả sử User kế thừa Item và có setId(String)
                    owner.setId(rs.getString("owner_id"));
                    product.setOwner(owner);

                    // Map timeCreated từ Timestamp sang LocalDateTime
                    Timestamp timeCreated = rs.getTimestamp("time_created");
                    if (timeCreated != null) {
                        product.setTimeCreated(timeCreated.toLocalDateTime());
                    }

                    products.add(product);
                }
            }

        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy danh sách sản phẩm broadcast: " + e.getMessage());
            e.printStackTrace();
        }

        return products;
    }
    // TÍNH NĂNG LÊN SÀN (SELL)
    public boolean sellProduct(String productId) {
        String sql = "UPDATE products SET status = 'ON_AUCTION', start_time = ?, end_time = ? WHERE id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // 1. start_time: Lấy ngay khoảnh khắc bấm nút
            pstmt.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()));

            // 2. end_time: Set vô hạn (Ví dụ: ngày 31/12/2099) để test
            pstmt.setTimestamp(2, java.sql.Timestamp.valueOf(java.time.LocalDateTime.of(2099, 12, 31, 23, 59, 59)));

            // 3. ID sản phẩm
            pstmt.setString(3, productId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("❌ [ProductDao] Lỗi khi đưa sản phẩm lên sàn: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

}