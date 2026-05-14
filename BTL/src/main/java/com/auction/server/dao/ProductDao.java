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
import java.util.List;
import java.util.ArrayList;

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
    /**
     * Tìm sản phẩm theo ID, nếu thấy thì cập nhật toàn bộ nội dung
     * y hệt đối tượng product truyền vào.
     */
    public boolean editProduct(Product product) {
        if (product == null || product.getId() == null) {
            return false;
        }

        // 1. Kiểm tra xem ID sản phẩm có tồn tại trong DB không
        Product existingProduct = getProductById(product.getId());

        if (existingProduct != null) {
            // 2. Nếu tìm thấy, tiến hành cập nhật nội dung
            String sql = "UPDATE products SET name = ?, category = ?, description = ?, " +
                    "start_price = ?, current_price = ?, step_price = ?, " +
                    "status = ?, owner_id = ?, time_created = ? " +
                    "WHERE id = ?";

            try (Connection conn = Db.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(sql)) {

                pstmt.setString(1, product.getName());
                pstmt.setString(2, product.getCategory());
                pstmt.setString(3, product.getDescription());
                pstmt.setDouble(4, product.getStartPrice());
                pstmt.setDouble(5, product.getCurrentPrice());
                pstmt.setDouble(6, product.getStepPrice());
                pstmt.setString(7, product.getStatus().toString());

                // Gán ID người sở hữu
                if (product.getOwner() != null) {
                    pstmt.setString(8, product.getOwner().getId());
                } else {
                    pstmt.setNull(8, java.sql.Types.VARCHAR);
                }

                pstmt.setTimestamp(9, Timestamp.valueOf(product.getTimeCreated()));

                // Điều kiện WHERE
                pstmt.setString(10, product.getId());

                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("[ProductDao] Cập nhật thành công sản phẩm ID: " + product.getId());
                    return true;
                }
            } catch (SQLException e) {
                System.err.println("[ProductDao] Lỗi khi thực hiện editProduct: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.out.println("[ProductDao] Không tìm thấy sản phẩm với ID: " + product.getId() + " để chỉnh sửa.");
        }

        return false;
    }
    /**
     * Lấy danh sách sản phẩm theo ID người dùng (owner_id)
     * @param userId ID của người dùng cần lấy danh sách sản phẩm
     * @return Danh sách các sản phẩm thuộc sở hữu của userId đó
     */
    public List<Product> getProductsByUserId(String userId) {
        List<Product> productList = new java.util.ArrayList<>();
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
                    product.setStartPrice(rs.getDouble("start_price"));
                    product.setCurrentPrice(rs.getDouble("current_price"));
                    product.setStepPrice(rs.getDouble("step_price"));

                    // Chuyển đổi status từ String trong DB sang Enum ProductStatus
                    product.setStatus(ProductStatus.valueOf(rs.getString("status")));

                    // Xử lý thời gian
                    Timestamp ts = rs.getTimestamp("time_created");
                    if (ts != null) {
                        product.setTimeCreated(ts.toLocalDateTime());
                    }

                    // Gán User sở hữu (chỉ cần set ID cho User object)
                    User owner = new User();
                    owner.setId(rs.getString("owner_id"));
                    product.setOwner(owner);

                    // Thêm sản phẩm vào danh sách trả về
                    productList.add(product);
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi khi lấy danh sách sản phẩm theo User ID: " + e.getMessage());
            e.printStackTrace();
        }

        return productList;
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