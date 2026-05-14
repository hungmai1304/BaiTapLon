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
import java.util.List;
import java.util.ArrayList;

public class ProductDao {

    private static ProductDao instance;

    private ProductDao() {}

    public static synchronized ProductDao getInstance() {
        if (instance == null) {
            instance = new ProductDao();
        }
        return instance;
    }

    public boolean saveProduct(Product product) {
        String sql = "INSERT INTO products (id, name, category, description, image_path, start_price, current_price, step_price, status, owner_id, time_created) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getId());
            pstmt.setString(2, product.getName());
            pstmt.setString(3, product.getCategory());
            pstmt.setString(4, product.getDescription());
            pstmt.setString(5, product.getImagePath());
            pstmt.setDouble(6, product.getStartPrice());
            pstmt.setDouble(7, product.getCurrentPrice());
            pstmt.setDouble(8, product.getStepPrice());
            pstmt.setString(9, product.getStatus().toString());
            pstmt.setString(10, product.getOwner().getId());
            pstmt.setTimestamp(11, Timestamp.valueOf(product.getTimeCreated()));

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Lỗi khi lưu sản phẩm: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public Product getProductById(String id) {
        String sql = "SELECT * FROM products WHERE id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Product product = new Product();
                    product.setId(rs.getString("id"));
                    product.setName(rs.getString("name"));
                    product.setCategory(rs.getString("category"));
                    product.setDescription(rs.getString("description"));
                    product.setImagePath(rs.getString("image_path"));
                    product.setStartPrice(rs.getDouble("start_price"));
                    product.setCurrentPrice(rs.getDouble("current_price"));
                    product.setStepPrice(rs.getDouble("step_price"));
                    product.setStatus(ProductStatus.valueOf(rs.getString("status")));

                    Timestamp ts = rs.getTimestamp("time_created");
                    if (ts != null) {
                        product.setTimeCreated(ts.toLocalDateTime());
                    }

                    User owner = new User();
                    owner.setId(rs.getString("owner_id"));
                    product.setOwner(owner);

                    return product;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateProduct(Product product) {
        String sql = "UPDATE products SET name = ?, category = ?, description = ?, image_path = ?, start_price = ?, " +
                "current_price = ?, step_price = ?, status = ?, owner_id = ?, time_created = ? " +
                "WHERE id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getCategory());
            pstmt.setString(3, product.getDescription());
            pstmt.setString(4, product.getImagePath());
            pstmt.setDouble(5, product.getStartPrice());
            pstmt.setDouble(6, product.getCurrentPrice());
            pstmt.setDouble(7, product.getStepPrice());
            pstmt.setString(8, product.getStatus().toString());
            pstmt.setString(9, product.getOwner().getId());
            pstmt.setTimestamp(10, Timestamp.valueOf(product.getTimeCreated()));
            pstmt.setString(11, product.getId());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean editProduct(Product product) {
        if (product == null || product.getId() == null) return false;

        String sql = "UPDATE products SET name = ?, category = ?, description = ?, image_path = ?, " +
                "start_price = ?, current_price = ?, step_price = ?, " +
                "status = ?, owner_id = ?, time_created = ? " +
                "WHERE id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getCategory());
            pstmt.setString(3, product.getDescription());
            pstmt.setString(4, product.getImagePath());
            pstmt.setDouble(5, product.getStartPrice());
            pstmt.setDouble(6, product.getCurrentPrice());
            pstmt.setDouble(7, product.getStepPrice());
            pstmt.setString(8, product.getStatus().toString());
            pstmt.setString(9, (product.getOwner() != null) ? product.getOwner().getId() : null);
            pstmt.setTimestamp(10, Timestamp.valueOf(product.getTimeCreated()));
            pstmt.setString(11, product.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Product> getProductsByUserId(String userId) {
        List<Product> productList = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE owner_id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, userId);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Product product = new Product();
                    product.setId(rs.getString("id"));
                    product.setName(rs.getString("name"));
                    product.setCategory(rs.getString("category"));
                    product.setDescription(rs.getString("description"));
                    product.setImagePath(rs.getString("image_path"));
                    product.setStartPrice(rs.getDouble("start_price"));
                    product.setCurrentPrice(rs.getDouble("current_price"));
                    product.setStepPrice(rs.getDouble("step_price"));
                    product.setStatus(ProductStatus.valueOf(rs.getString("status")));

                    Timestamp ts = rs.getTimestamp("time_created");
                    if (ts != null) {
                        product.setTimeCreated(ts.toLocalDateTime());
                    }

                    User owner = new User();
                    owner.setId(rs.getString("owner_id"));
                    product.setOwner(owner);

                    productList.add(product);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productList;
    }

    public boolean sellProduct(String productId) {
        String sql = "UPDATE products SET status = 'ON_AUCTION', start_time = ?, end_time = ? WHERE id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            pstmt.setTimestamp(2, Timestamp.valueOf(java.time.LocalDateTime.of(2099, 12, 31, 23, 59, 59)));
            pstmt.setString(3, productId);

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}