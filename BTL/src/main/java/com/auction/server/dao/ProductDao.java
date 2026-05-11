package com.auction.server.dao;
// file này sửa thoải mái
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
    // PHẦN CODE MỚI BỔ SUNG CHO TÍNH NĂNG "LÊN SÀN" (SELL PRODUCT)

    public Product getProductById(String id) {
        String sql = "SELECT * FROM products WHERE id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) { // Nếu tìm thấy hàng trong DB
                    Product product = new Product();
                    product.setId(rs.getString("id"));
                    product.setName(rs.getString("name"));
                    product.setCategory(rs.getString("category"));
                    product.setDescription(rs.getString("description"));
                    product.setStartPrice(rs.getDouble("start_price"));
                    product.setCurrentPrice(rs.getDouble("current_price"));
                    product.setStepPrice(rs.getDouble("step_price"));

                    // Chuyển chuỗi SQL thành Enum ProductStatus
                    product.setStatus(ProductStatus.valueOf(rs.getString("status")));

                    // Chuyển Timestamp từ SQL về LocalDateTime
                    Timestamp ts = rs.getTimestamp("time_created");
                    if (ts != null) {
                        product.setTimeCreated(ts.toLocalDateTime());
                    }

                    // Gán Owner ID (Tạo 1 User ảo để chứa ID)
                    User owner = new User();
                    owner.setId(rs.getString("owner_id"));
                    product.setOwner(owner);

                    return product;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ [ProductDao] Lỗi khi lấy sản phẩm theo ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // Không tìm thấy hoặc lỗi
    }

    // Hàm cập nhật thông tin sản phẩm (Dùng để đổi trạng thái sang ON_AUCTION)
    public boolean updateProduct(Product product) {
        // Cập nhật tất cả các trường phòng trường hợp có Edit Product sau này
        String sql = "UPDATE products SET name = ?, category = ?, description = ?, start_price = ?, " +
                "current_price = ?, step_price = ?, status = ?, owner_id = ?, time_created = ? " +
                "WHERE id = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, product.getName());
            pstmt.setString(2, product.getCategory());
            pstmt.setString(3, product.getDescription());
            pstmt.setDouble(4, product.getStartPrice());
            pstmt.setDouble(5, product.getCurrentPrice());
            pstmt.setDouble(6, product.getStepPrice());
            pstmt.setString(7, product.getStatus().toString()); // Trạng thái mới (VD: ON_AUCTION)
            pstmt.setString(8, product.getOwner().getId());
            pstmt.setTimestamp(9, Timestamp.valueOf(product.getTimeCreated()));

            // Điều kiện WHERE id = ?
            pstmt.setString(10, product.getId());

            return pstmt.executeUpdate() > 0; // Trả về true nếu có ít nhất 1 dòng được cập nhật thành công

        } catch (SQLException e) {
            System.err.println("❌ [ProductDao] Lỗi khi cập nhật sản phẩm: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}