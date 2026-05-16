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

            // 2. Map data từ Client sang Object Product (Dùng Gson để chuẩn hóa dữ liệu)
            // Việc map qua JsonTree giúp tránh lỗi ép kiểu Number của Map
            String jsonData = gson.toJson(data);
            Product product = gson.fromJson(jsonData, Product.class);

            // 3. Xử lý ảnh (Chỉ xử lý, không in ra terminal)
            String imageBase64 = product.getImageBase64();
            if (imageBase64 != null && !imageBase64.isEmpty()) {
                // Upload lên Cloudinary thông qua FileService
                String imageUrl = FileService.saveImage(imageBase64, product.getId());

                if (imageUrl != null) {
                    product.setImagePath(imageUrl);
                } else {
                    sendError(conn, gson, "Không thể upload ảnh lên Cloud.");
                    return;
                }
            }

            // 4. Thiết lập các thông số mặc định trước khi lưu
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            product.setOwner(currentUser);
            product.setStatus(ProductStatus.AVAILABLE);
            product.setTimeCreated(LocalDateTime.now());
            product.setCurrentPrice(product.getStartPrice());
            if (product.getId() == null || product.getId().trim().isEmpty()) {
                product.setId(java.util.UUID.randomUUID().toString());
            }

            // Xóa Base64 ngay lập tức để giải phóng RAM
            product.setImageBase64(null);

            // 5. Lưu vào Database và Context
            boolean isSaved = ProductDao.getInstance().saveProduct(product);
            if (isSaved) {
                context.addProduct(product);

                // Gửi phản hồi thành công (Chỉ in link ảnh ngắn gọn)
                Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "SUCCESS", "Sản phẩm đã được lưu!");
                conn.send(gson.toJson(response));
                System.out.println("[Server] Import thành công SP: " + product.getName() + " -> " + product.getImagePath());
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