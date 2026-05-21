package com.auction.server.dao;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;
import java.sql.*;
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

    // 1. LẤY TẤT CẢ SẢN PHẨM (Để nạp vào ServerContext lúc khởi động)
    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        String sql = "SELECT * FROM products";
        try (Connection conn = Db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // 2. LƯU SẢN PHẨM MỚI
    public boolean saveProduct(Product product) {
        // Thêm 2 cột vào SQL
        String sql = "INSERT INTO products (id, name, category, description, image_path, start_price, " +
                "current_price, step_price, status, owner_id, time_created, start_time, end_time) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

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
            pstmt.setString(10, (product.getOwner() != null) ? product.getOwner().getId() : null);

            pstmt.setTimestamp(11, (product.getTimeCreated() != null) ? Timestamp.valueOf(product.getTimeCreated()) : null);

            // Thêm 2 tham số mới (Kiểm tra null để tránh crash)
            pstmt.setTimestamp(12, (product.getStartTime() != null) ? Timestamp.valueOf(product.getStartTime()) : null);
            pstmt.setTimestamp(13, (product.getEndTime() != null) ? Timestamp.valueOf(product.getEndTime()) : null);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi saveProduct: " + e.getMessage());
            return false;
        }
    }

    // 3. LẤY THEO ID
    public Product getProductById(String id) {
        String sql = "SELECT * FROM products WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToProduct(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    // 4. CHỈNH SỬA SẢN PHẨM (Dùng chung cho cả update thông thường và EditHandler)
    public boolean editProduct(Product product) {
        if (product == null || product.getId() == null) return false;

        String sql = "UPDATE products SET name = ?, category = ?, description = ?, image_path = ?, " +
                "start_price = ?, current_price = ?, step_price = ?, " +
                "status = ?, owner_id = ?, time_created = ?, start_time = ?, end_time = ? " +
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
            pstmt.setTimestamp(10, (product.getTimeCreated() != null) ? Timestamp.valueOf(product.getTimeCreated()) : null);

            // Thêm 2 tham số mới
            pstmt.setTimestamp(11, (product.getStartTime() != null) ? Timestamp.valueOf(product.getStartTime()) : null);
            pstmt.setTimestamp(12, (product.getEndTime() != null) ? Timestamp.valueOf(product.getEndTime()) : null);

            pstmt.setString(13, product.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // 5. LẤY DANH SÁCH THEO USER
    public List<Product> getProductsByUserId(String userId) {
        List<Product> productList = new ArrayList<>();
        String sql = "SELECT * FROM products WHERE owner_id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    productList.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return productList;
    }

    // 6. CHUYỂN TRẠNG THÁI ĐẤU GIÁ
    public boolean sellProduct(String productId) {
        String sql = "UPDATE products SET status = 'ON_AUCTION', start_time = ?, end_time = ? WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Tính toán thời gian y hệt như trên RAM
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            java.time.LocalDateTime startTime = now.plusMinutes(1); // Chờ 30p
            java.time.LocalDateTime endTime = startTime.plusMinutes(2); // Đấu 10p , test thì giây cho nhanh

            pstmt.setTimestamp(1, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(2, Timestamp.valueOf(endTime));
            pstmt.setString(3, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    // HÀM PHỤ TRỢ: Chuyển dữ liệu từ Database sang Object Java (Tránh viết lặp code)
    private Product mapResultSetToProduct(ResultSet rs) throws SQLException {
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

        String ownerId = rs.getString("owner_id");
        if (ownerId != null) {
            User owner = UserDao.getInstance().findById(ownerId);
            if (owner != null) {
                owner.setPassword(null); // Bảo mật: Không gửi password của owner qua mạng
                product.setOwner(owner);
            } else {
                User fallbackOwner = new User();
                fallbackOwner.setId(ownerId);
                product.setOwner(fallbackOwner);
            }
        }
        Timestamp startTimeTs = rs.getTimestamp("start_time");
        if (startTimeTs != null) {
            product.setStartTime(startTimeTs.toLocalDateTime());
        }

        // Đọc thêm end_time
        Timestamp endTimeTs = rs.getTimestamp("end_time");
        if (endTimeTs != null) {
            product.setEndTime(endTimeTs.toLocalDateTime());
        }

        return product;

    }
    public List<Product> getProductsByUserEmail(String email) {
        List<Product> productList = new ArrayList<>();
        // Dùng JOIN để lấy sản phẩm dựa trên email của chủ sở hữu
        String sql = "SELECT p.* FROM products p " +
                "JOIN users u ON p.owner_id = u.id " +
                "WHERE u.email = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Tận dụng lại hàm map dặm có sẵn của mày
                    productList.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Lỗi khi lấy sản phẩm theo email: " + e.getMessage());
            e.printStackTrace();
        }
        return productList;
    }

    // 7. XÓA SẢN PHẨM KHỎI DATABASE
    public boolean deleteProduct(String productId) {
        String sql = "DELETE FROM products WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, productId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi xóa sản phẩm: " + e.getMessage());
            return false;
        }
    }
}