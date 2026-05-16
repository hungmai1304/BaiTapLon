package com.auction.server.handler;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.auction.server.service.FileService;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@CommandMap(value = MessageType.IMPORT_PRODUCT_REQUEST)
public class ImportProductRequestHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. Kiểm tra đăng nhập
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập!");
                return;
            }

            // 2. Map data từ Client sang Object Product
            String jsonData = gson.toJson(data);
            Product product = gson.fromJson(jsonData, Product.class);

            // --- SỬA Ở ĐÂY: KHỞI TẠO ID TRƯỚC ĐỂ LÀM TÊN FILE TRÊN CLOUDINARY ---
            if (product.getId() == null || product.getId().trim().isEmpty()) {
                // Tạo một ID duy nhất, ví dụ cắt ngắn bớt cho tên file Cloudinary đỡ dài (tùy chọn)
                String generatedId = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
                product.setId(generatedId);
            }

            // 3. Xử lý ảnh (Đẩy lên Cloudinary)
            String imageBase64 = product.getImageBase64();
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                // Lúc này product.getId() chắc chắn đã có giá trị
                String imageUrl = FileService.saveImage(imageBase64, product.getId());
                if (imageUrl != null) {
                    product.setImagePath(imageUrl); // Lưu Link Cloudinary vào DB
                } else {
                    sendError(conn, gson, "Không thể upload ảnh lên Cloud.");
                    return;
                }
            }

            // 4. Thiết lập các thông số mặc định
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            product.setOwner(currentUser);
            product.setStatus(ProductStatus.AVAILABLE);
            product.setTimeCreated(LocalDateTime.now());
            product.setCurrentPrice(product.getStartPrice());

            // Đảm bảo start_time và end_time là null khi mới import
            product.setStartTime(null);
            product.setEndTime(null);

            // Giải phóng RAM (Xóa chuỗi Base64 đi vì đã có link URL, không cần lưu vào DB)
            product.setImageBase64(null);

            // 5. Lưu vào Database và Context
            boolean isSaved = ProductDao.getInstance().saveProduct(product);

            if (isSaved) {
                context.addProduct(product);

                // Gửi phản hồi thành công
                Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "SUCCESS", "Sản phẩm đã được lưu!");
                conn.send(gson.toJson(response));
                System.out.println("[Server] Import thành công SP: " + product.getName() + " | ID: " + product.getId());
            } else {
                sendError(conn, gson, "Lỗi lưu Database.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}