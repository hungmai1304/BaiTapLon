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
import com.auction.server.service.FileService; // Nhớ check package utils hay service nhé
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.util.Map;

@CommandMap(value = MessageType.IMPORT_PRODUCT_REQUEST)
public class ImportProductRequestHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. LẤY THÔNG TIN NGƯỜI GỬI
            String userEmail = context.getUserByConn(conn);

            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập để thực hiện chức năng này!");
                return;
            }

            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);

            // 2. LẤY DỮ LIỆU TỪ CLIENT
            String id = (String) data.get("id");
            String name = (String) data.get("name");
            String category = (String) data.get("category");
            String description = (String) data.get("description");
            String imageBase64 = (String) data.get("imageBase64");

            double startPrice = ((Number) data.get("startPrice")).doubleValue();
            double stepPrice = ((Number) data.get("stepPrice")).doubleValue();

            // --- BẮT ĐẦU XỬ LÝ CLOUDINARY ---
            // Gọi FileService để upload trực tiếp lên Cloudinary
            // Trả về link URL ví dụ: https://res.cloudinary.com/dlylyya7s/image/upload/...
            String imageUrl = FileService.saveImage(imageBase64, id);

            if (imageUrl == null && imageBase64 != null && !imageBase64.isEmpty()) {
                sendError(conn, gson, "Lỗi khi upload ảnh lên Cloudinary!");
                return;
            }
            // ------------------------------

            // 3. TẠO ĐỐI TƯỢNG PRODUCT
            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setCategory(category);
            product.setDescription(description);

            // QUAN TRỌNG: Lưu URL của Cloudinary vào Database
            product.setImagePath(imageUrl);

            // Xóa Base64 cho nhẹ RAM Server
            product.setImageBase64(null);

            product.setStartPrice(startPrice);
            product.setCurrentPrice(startPrice);
            product.setStepPrice(stepPrice);
            product.setStatus(ProductStatus.AVAILABLE);
            product.setOwner(currentUser);
            product.setTimeCreated(LocalDateTime.now());

            // 4. LƯU VÀO DATABASE VÀ CONTEXT
            // ProductDao.saveProduct sẽ lưu cột image_path vào Postgres
            boolean isSaved = ProductDao.getInstance().saveProduct(product);

            if (isSaved) {
                context.addProduct(product);

                Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "SUCCESS", "Đã lưu sản phẩm và ảnh lên Cloud thành công!");
                conn.send(gson.toJson(response));
                System.out.println("[ImportProduct] Thành công! Link ảnh: " + imageUrl);
            } else {
                sendError(conn, gson, "Không thể lưu sản phẩm vào Database!");
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