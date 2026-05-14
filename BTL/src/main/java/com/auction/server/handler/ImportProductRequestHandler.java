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
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.util.Map;

@CommandMap(value = MessageType.IMPORT_PRODUCT_REQUEST)
public class ImportProductRequestHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. LẤY THÔNG TIN NGƯỜI GỬI (OWNER)
            String userEmail = context.getUserByConn(conn);

            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập để thực hiện chức năng này!");
                return;
            }

            // Truy xuất thông tin User đầy đủ từ Database
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);

            // 2. LẤY DỮ LIỆU SẢN PHẨM TỪ CLIENT (MAP data)
            String id = (String) data.get("id");
            String name = (String) data.get("name");
            String category = (String) data.get("category");
            String description = (String) data.get("description");
            String imageBase64 = (String) data.get("imageBase64"); // Nhận dữ liệu ảnh base64

            // Ép kiểu số an toàn (Gson thường parse số thành Double)
            double startPrice = ((Number) data.get("startPrice")).doubleValue();
            double stepPrice = ((Number) data.get("stepPrice")).doubleValue();

            // 3. TẠO ĐỐI TƯỢNG PRODUCT VÀ GÁN OWNER
            Product product = new Product();
            product.setId(id);
            product.setName(name);
            product.setCategory(category);
            product.setDescription(description);
            product.setImageBase64(imageBase64); // Gán ảnh base64 vào đối tượng Product
            product.setStartPrice(startPrice);
            product.setCurrentPrice(startPrice); // Giá hiện tại mới tạo bằng giá khởi điểm
            product.setStepPrice(stepPrice);
            product.setStatus(ProductStatus.AVAILABLE);

            // GÁN OWNER CHÍNH LÀ NGƯỜI ĐANG KẾT NỐI
            product.setOwner(currentUser);

            // Set thời gian tạo
            product.setTimeCreated(LocalDateTime.now());

            // 4. LƯU VÀO DATABASE VÀ CONTEXT
            boolean isSaved = ProductDao.getInstance().saveProduct(product);

            if (isSaved) {
                // Thêm vào danh sách quản lý của Server
                context.addProduct(product);

                Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "SUCCESS", "Đã lưu sản phẩm thành công!");
                conn.send(gson.toJson(response));
                System.out.println("[ImportProduct] User " + userEmail + " đã thêm SP: " + name);
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