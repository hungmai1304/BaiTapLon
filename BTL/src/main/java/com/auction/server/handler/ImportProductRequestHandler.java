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
import com.auction.server.service.FileService; // Thêm import file service
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

            // --- BẮT ĐẦU XỬ LÝ FILE ẢNH ---
            // Gọi service để lưu ảnh vào thư mục server_data/product_images/
            // Nhận lại đường dẫn String (vd: "server_data/product_images/SP01.png")
            String savedPath = FileService.saveImage(imageBase64, id);
            // ------------------------------

            // 3. TẠO ĐỐI TƯỢNG PRODUCT
            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setCategory(category);
            product.setDescription(description);

            // QUAN TRỌNG: Lưu đường dẫn file vào Database thay vì Base64
            product.setImagePath(savedPath);

            // Xóa Base64 để nhẹ RAM của Server khi lưu vào Context
            product.setImageBase64(null);

            product.setStartPrice(startPrice);
            product.setCurrentPrice(startPrice);
            product.setStepPrice(stepPrice);
            product.setStatus(ProductStatus.AVAILABLE);
            product.setOwner(currentUser);
            product.setTimeCreated(LocalDateTime.now());

            // 4. LƯU VÀO DATABASE VÀ CONTEXT
            // ProductDao.saveProduct cần được cập nhật để lưu cột 'image_path'
            boolean isSaved = ProductDao.getInstance().saveProduct(product);

            if (isSaved) {
                context.addProduct(product);

                Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "SUCCESS", "Đã lưu sản phẩm và ảnh thành công!");
                conn.send(gson.toJson(response));
                System.out.println("[ImportProduct] User " + userEmail + " đã thêm SP: " + name + " (Ảnh: " + savedPath + ")");
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