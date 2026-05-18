package com.auction.server.handler;

import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.auction.server.service.FileService;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;

@CommandMap(value = MessageType.EDIT_PRODUCT_REQUEST)
public class EditProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. Kiểm tra đăng nhập (Lấy email người dùng từ conn)
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập!");
                return;
            }

            // 2. Lấy ID sản phẩm cần sửa
            String productId = (String) data.get("id");
            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Mã sản phẩm không hợp lệ!");
                return;
            }

            // Tìm sản phẩm gốc trong DB
            Product productToEdit = ProductDao.getInstance().getProductById(productId);
            if (productToEdit == null) {
                sendError(conn, gson, "Sản phẩm không tồn tại!");
                return;
            }

            // Kiểm tra quyền sở hữu: Email người sửa phải là chủ sản phẩm
            if (!productToEdit.getOwner().getEmail().equals(userEmail)) {
                sendError(conn, gson, "Bạn không có quyền sửa sản phẩm này!");
                return;
            }

            // 3. Cập nhật thông tin mới từ data map gửi lên
            if (data.containsKey("name")) productToEdit.setName((String) data.get("name"));
            if (data.containsKey("category")) productToEdit.setCategory((String) data.get("category"));
            if (data.containsKey("description")) productToEdit.setDescription((String) data.get("description"));

            if (data.containsKey("startPrice")) {
                double newPrice = ((Number) data.get("startPrice")).doubleValue();
                productToEdit.setStartPrice(newPrice);
                productToEdit.setCurrentPrice(newPrice);
            }
            if (data.containsKey("stepPrice")) {
                productToEdit.setStepPrice(((Number) data.get("stepPrice")).doubleValue());
            }

            // 4. Xử lý ảnh mới (Đẩy lên Cloudinary thông qua FileService nếu có)
            if (data.containsKey("imageBase64")) {
                String newImageBase64 = (String) data.get("imageBase64");
                if (newImageBase64 != null && !newImageBase64.isEmpty()) {
                    // Trả về link URL Cloudinary mới và set thẳng vào đối tượng
                    String newUrl = FileService.saveImage(newImageBase64, productId);
                    if (newUrl != null) productToEdit.setImagePath(newUrl);
                }
            }

            // 5. Lưu trực tiếp vào DB (Bỏ qua đoạn cập nhật RAM context.updateProduct)
            boolean isUpdated = ProductDao.getInstance().editProduct(productToEdit);

            if (isUpdated) {
                // Lấy lại toàn bộ danh sách sản phẩm mới nhất thuộc về Shop của user này từ DB
                List<Product> updatedList = ProductDao.getInstance().getProductsByUserEmail(userEmail);

                // Đóng gói và gửi phản hồi cá nhân về cho Client yêu cầu chỉnh sửa
                Response response = new Response(
                        MessageType.EDIT_PRODUCT_RESPONSE,
                        "SUCCESS",
                        "Cập nhật thành công!"
                );

                // Đặt trực tiếp danh sách sản phẩm vừa cập nhật vào Map data để hiển thị lại UI "My Shop"
                response.getData().put("products", updatedList);

                conn.send(gson.toJson(response));
                System.out.println("[EditProduct] Đã sửa thành công dưới DB và gửi lại danh sách Shop cho user: " + userEmail);

            } else {
                sendError(conn, gson, "Lỗi cập nhật Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.EDIT_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}