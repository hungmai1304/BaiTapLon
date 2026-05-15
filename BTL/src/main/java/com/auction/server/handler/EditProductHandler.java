package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
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

            // Tìm trong DB
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

            // 3. Cập nhật thông tin từ data gửi lên
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

            // 4. Xử lý ảnh mới (Cloudinary)
            if (data.containsKey("imageBase64")) {
                String newImageBase64 = (String) data.get("imageBase64");
                if (newImageBase64 != null && !newImageBase64.isEmpty()) {
                    String newUrl = FileService.saveImage(newImageBase64, productId);
                    if (newUrl != null) productToEdit.setImagePath(newUrl);
                }
            }

            // 5. Lưu vào DB và RAM
            boolean isUpdated = ProductDao.getInstance().editProduct(productToEdit);

            if (isUpdated) {
                context.updateProduct(productToEdit); // Cập nhật RAM

                // --- PHẦN MÀY CẦN: GỬI LẠI DANH SÁCH MỚI ---

                // Lấy lại toàn bộ danh sách sản phẩm của Shop này (dựa trên email chủ sở hữu)
                List<Product> updatedList = ProductDao.getInstance().getProductsByUserEmail(userEmail);

                // Gửi phản hồi thành công kèm danh sách sản phẩm mới
                Response response = new Response(
                        MessageType.EDIT_PRODUCT_RESPONSE,
                        "SUCCESS",
                        "Cập nhật thành công!"
                );
                response.getData().put("products", updatedList);

                conn.send(gson.toJson(response));
                System.out.println("[EditProduct] Đã gửi lại danh sách mới cho user: " + userEmail);

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