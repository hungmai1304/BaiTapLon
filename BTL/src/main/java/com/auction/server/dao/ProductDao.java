package com.auction.server.dao;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.server.db.Db;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

public class ProductDao {

    private static ProductDao instance;

    // TỐI ƯU: Định nghĩa sẵn một câu SELECT mẫu dùng LEFT JOIN để tái sử dụng, triệt tiêu hoàn toàn lỗi N+1 Query
    private static final String SELECT_PRODUCT_WITH_OWNER =
            "SELECT p.*, " +
                    "       u.name AS owner_name, u.email AS owner_email, u.role AS owner_role, " +
                    "       u.balance AS owner_balance, u.status AS owner_status, u.shop_name AS owner_shop_name, " +
                    "       u.avatar AS owner_avatar, u.time_created AS owner_time_created " +
                    "FROM products p " +
                    "LEFT JOIN users u ON p.owner_id = u.id";

    private ProductDao() {}

    public static synchronized ProductDao getInstance() {
        if (instance == null) {
            instance = new ProductDao();
        }
        return instance;
    }

    /**
     * 1. LẤY TẤT CẢ SẢN PHẨM - Đã tối ưu bằng 1 câu lệnh JOIN duy nhất
     */
    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        try (Connection conn = Db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_PRODUCT_WITH_OWNER)) {
            while (rs.next()) {
                list.add(mapResultSetToProduct(rs));
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi getAllProducts: " + e.getMessage());
        }
        return list;
    }

    /**
     * 2. LƯU SẢN PHẨM MỚI
     */
    public boolean saveProduct(Product product) {
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
            pstmt.setTimestamp(12, (product.getStartTime() != null) ? Timestamp.valueOf(product.getStartTime()) : null);
            pstmt.setTimestamp(13, (product.getEndTime() != null) ? Timestamp.valueOf(product.getEndTime()) : null);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi saveProduct: " + e.getMessage());
            return false;
        }
    }

    /**
     * 3. LẤY THEO ID - Đã tối ưu JOIN
     */
    public Product getProductById(String id) {
        String sql = SELECT_PRODUCT_WITH_OWNER + " WHERE p.id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapResultSetToProduct(rs);
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi getProductById: " + e.getMessage());
        }
        return null;
    }

    /**
     * 4. CHỈNH SỬA SẢN PHẨM
     */
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
            pstmt.setTimestamp(11, (product.getStartTime() != null) ? Timestamp.valueOf(product.getStartTime()) : null);
            pstmt.setTimestamp(12, (product.getEndTime() != null) ? Timestamp.valueOf(product.getEndTime()) : null);
            pstmt.setString(13, product.getId());

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi editProduct: " + e.getMessage());
            return false;
        }
    }

    /**
     * 5. HÀM LẤY SẢN PHẨM CỦA SHOP DỰA TRÊN ID NGUWLWIF DÙNG: CHƯA DÙNG
     */
    public List<Product> getProductsByUserId(String userId) {
        List<Product> productList = new ArrayList<>();
        String sql = SELECT_PRODUCT_WITH_OWNER + " WHERE p.owner_id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    productList.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi getProductsByUserId: " + e.getMessage());
        }
        return productList;
    }

    /**
     * 6. CHUYỂN TRẠNG THÁI SẢN PHẨM SANG ĐANG ĐẤU GIÁ (ĐÃ ĐỘNG HÓA NGHIỆP VỤ)
     * Khi đưa lên sàn, cập nhật lại: Giá hiện tại = Giá bắt đầu, set thời gian sống động cho phiên.
     */
    public boolean sellProduct(String productId, double startPrice, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = "UPDATE products SET status = 'ON_AUCTION', start_price = ?, current_price = ?, start_time = ?, end_time = ? WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setDouble(1, startPrice);
            pstmt.setDouble(2, startPrice); // Lúc vừa mở sàn, current_price mặc định bằng start_price
            pstmt.setTimestamp(3, Timestamp.valueOf(startTime));
            pstmt.setTimestamp(4, Timestamp.valueOf(endTime));
            pstmt.setString(5, productId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi sellProduct: " + e.getMessage());
            return false;
        }
    }

    /**
     * NGHIỆP VỤ BỔ SUNG: Cập nhật nhanh trạng thái sản phẩm (ví dụ: SOLD, COMPLETED, PENDING) CHO ADMIN
     */
    // Trong lớp ProductDao của bạn
    public boolean updateProductStatus(String productId, ProductStatus status) {
        String sql = "UPDATE products SET status = ? WHERE id = ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, status.name()); // Dùng .name() sẽ an toàn hơn .toString()
            pstmt.setString(2, productId);

            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi updateProductStatus: " + e.getMessage());
            return false;
        }
    }

    /**
     * 7. LẤY DANH SÁCH THEO USER EMAIL - Đã tối ưu JOIN
     */
    public List<Product> getProductsByUserEmail(String email) {
        List<Product> productList = new ArrayList<>();
        String sql = SELECT_PRODUCT_WITH_OWNER + " WHERE u.email = ?";

        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    productList.add(mapResultSetToProduct(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi getProductsByUserEmail: " + e.getMessage());
        }
        return productList;
    }

    /**
     * 8. XÓA SẢN PHẨM KHỎI DATABASE
     */
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

    /**
     * hàm map
     */
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
        if (ts != null) product.setTimeCreated(ts.toLocalDateTime());

        Timestamp startTimeTs = rs.getTimestamp("start_time");
        if (startTimeTs != null) product.setStartTime(startTimeTs.toLocalDateTime());

        Timestamp endTimeTs = rs.getTimestamp("end_time");
        if (endTimeTs != null) product.setEndTime(endTimeTs.toLocalDateTime());

        String ownerId = rs.getString("owner_id");
        if (ownerId != null) {
            String ownerEmail = rs.getString("owner_email");

            if (ownerEmail != null) {
                String ownerRole = rs.getString("owner_role");
                User owner;

                if ("SELLER".equalsIgnoreCase(ownerRole)) {
                    Seller seller = new Seller();
                    seller.setShopName(rs.getString("owner_shop_name"));
                    owner = seller;
                } else if ("BIDDER".equalsIgnoreCase(ownerRole)) {
                    owner = new Bidder();
                } else if ("ADMIN".equalsIgnoreCase(ownerRole)) {
                    owner = new Admin();
                } else {
                    owner = new User();
                }

                owner.setId(ownerId);
                owner.setEmail(ownerEmail);
                owner.setUsername(rs.getString("owner_name"));
                owner.setRole(ownerRole);
                owner.setBalance(rs.getDouble("owner_balance"));
                owner.setStatus(rs.getString("owner_status"));
                owner.setPassword(null);

                byte[] avatarBytes = rs.getBytes("owner_avatar");
                if (avatarBytes != null) {
                    owner.setAvatar(Base64.getEncoder().encodeToString(avatarBytes));
                }

                Timestamp ownerTs = rs.getTimestamp("owner_time_created");
                if (ownerTs != null) owner.setTimeCreated(ownerTs.toLocalDateTime());

                product.setOwner(owner);
            } else {
                User fallbackOwner = new User();
                fallbackOwner.setId(ownerId);
                product.setOwner(fallbackOwner);
            }
        }
        return product;
    }
    // tao 1 conbot , 10s chay 1 lan, luon check xem product nao co endtime qua thoi gian hien tai roi thi doi status sang AVAILABLE
    /**
     * NGHIỆP VỤ BOT: Tự động quét và hạ sàn các sản phẩm đã quá giờ kết thúc
     * Chuyển trạng thái từ 'ON_AUCTION' sang 'AVAILABLE'
     * @return Số lượng sản phẩm đã được xử lý thành công
     */
    public int autoExpireProducts() {
        String sql = "UPDATE products SET status = 'AVAILABLE' WHERE status = 'ON_AUCTION' AND end_time <= ?";
        try (Connection conn = Db.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            // Truyền mốc thời gian hiện tại của Server vào câu lệnh
            pstmt.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));

            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("[ProductBot] Đã hạ sàn thành công " + rowsAffected + " sản phẩm hết hạn.");
            }
            return rowsAffected;
        } catch (SQLException e) {
            System.err.println("[ProductDao] Lỗi thực thi autoExpireProducts: " + e.getMessage());
            return 0;
        }
    }
}