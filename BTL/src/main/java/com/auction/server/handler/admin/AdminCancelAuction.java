package com.auction.server.handler.admin;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.User;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.Map;

@CommandMap("ADMIN_CANCEL_AUCTION")
public class AdminCancelAuction implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        // 1. Khởi tạo Map gói tin phản hồi công khai
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "ADMIN_CANCEL_AUCTION_RESPONSE");

        // =========================================================================
        // KIỂM TRA QUYỀN TRUY CẬP (Phân quyền Admin tối cao)
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminCancelAuction] Từ chối: Kết nối này chưa đăng nhập hệ thống!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminCancelAuction] Cảnh báo nguy hiểm: Tài khoản [" + adminEmail + "] cố gắng can thiệp hủy đấu giá công khai!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền hạn tối cao để thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. TIẾN HÀNH TRÍCH XUẤT DATA VÀ XỬ LÝ HỦY PHIÊN ĐẤU GIÁ
        // =========================================================================
        try {
            // Lấy productId gửi lên từ Client
            String targetId = (String) data.get("productId");
            if (targetId == null || targetId.trim().isEmpty()) {
                responseMap.put("status", "ERROR");
                responseMap.put("message", "Dữ liệu yêu cầu không hợp lệ (Thiếu ID mục tiêu)!");
                conn.send(gson.toJson(responseMap));
                return;
            }

            System.out.println("[AdminCancelAuction] Admin [" + adminEmail + "] đang yêu cầu hủy đấu giá cho ID: " + targetId);

            Auction targetAuction = null;

            // Tìm kiếm phiên đấu giá tương ứng trên RAM để xử lý đồng bộ hóa luồng (Thread-safe)
            synchronized (context.getActiveAuctions()) {
                for (Auction a : context.getActiveAuctions()) {
                    if (a != null && (targetId.equals(a.getId()) || (a.getProduct() != null && targetId.equals(a.getProduct().getId())))) {
                        targetAuction = a;
                        break;
                    }
                }

                // Xóa khỏi danh sách đấu giá trên RAM thông qua ServerContext
                if (targetAuction != null) {
                    // Mẹo xử lý: Chuyền chính xác tham chiếu ID gốc để bypass lỗi so sánh '==' trong hàm removeAuction của ServerContext
                    context.removeAuction(targetAuction.getId());
                }
            }

            // =========================================================================
            // 3. CẬP NHẬT TRẠNG THÁI SẢN PHẨM TRONG DATABASE (PRODUCT DAO)
            // =========================================================================
            ProductDao productDao = ProductDao.getInstance();
            // Xác định chính xác ID sản phẩm cần cập nhật trong DB
            String dbProductId = (targetAuction != null && targetAuction.getProduct() != null)
                    ? targetAuction.getProduct().getId()
                    : targetId;

            Product product = productDao.getProductById(dbProductId);
            boolean dbUpdated = false;

            if (product != null) {
                // Đổi trạng thái thành NOT_AVAILABLE theo yêu cầu bài toán
                product.setStatus(ProductStatus.NOT_AVAILABLE);
                dbUpdated = productDao.editProduct(product);
            }

            // =========================================================================
            // 4. TRẢ KẾT QUẢ VỀ CHO CLIENT ADMIN
            // =========================================================================
            if (targetAuction != null || dbUpdated) {
                responseMap.put("status", "SUCCESS");
                responseMap.put("message", "Hệ thống đã can thiệp dừng cuộc họp đấu giá & cập nhật trạng thái sản phẩm!");
                System.out.println("[AdminCancelAuction] Hủy thành công phiên đấu giá liên quan đến ID [" + dbProductId + "] bởi Admin.");
            } else {
                responseMap.put("status", "ERROR");
                responseMap.put("message", "Không tìm thấy phiên đấu giá hoặc sản phẩm tương ứng trên hệ thống DB!");
            }

            conn.send(gson.toJson(responseMap));

        } catch (Exception e) {
            System.err.println("[AdminCancelAuction] Lỗi hệ thống phát sinh: " + e.getMessage());
            e.printStackTrace();

            responseMap.put("status", "ERROR");
            responseMap.put("message", "Có lỗi xảy ra trong quá trình hủy phiên đấu giá trên Server!");
            conn.send(gson.toJson(responseMap));
        }
    }
}