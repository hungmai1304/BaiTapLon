package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.auction.server.service.FileService; // Thêm import
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

@CommandMap(value = MessageType.EDIT_PRODUCT_REQUEST)
public class EditProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. Lấy ID từ data
            String productId = (String) data.get("id");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Không tìm thấy mã sản phẩm để sửa!");
                return;
            }

            // Tìm sản phẩm hiện tại trong DB
            Product productToEdit = ProductDao.getInstance().getProductById(productId);

            if (productToEdit == null) {
                sendError(conn, gson, "Sản phẩm không tồn tại trên hệ thống!");
                return;
            }

            // 2. Cập nhật các thông tin text
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

            // --- 3. XỬ LÝ ẢNH (QUAN TRỌNG) ---
            if (data.containsKey("imageBase64")) {
                String newImageBase64 = (String) data.get("imageBase64");

                if (newImageBase64 != null && !newImageBase64.isEmpty()) {
                    // Gọi FileService để lưu (nó sẽ ghi đè lên file .png cũ của productId này)
                    String newPath = FileService.saveImage(newImageBase64, productId);
                    productToEdit.setImagePath(newPath);
                }
            }
            // --------------------------------

            // 4. LƯU XUỐNG DATABASE
            boolean isUpdated = ProductDao.getInstance().editProduct(productToEdit);

            if (isUpdated) {
                // Cập nhật lại trong ServerContext (RAM) nếu sản phẩm này đang online
                // Dù mày bảo "đéo cần" nhưng nếu không update RAM thì các client khác
                // đang xem đấu giá sẽ vẫn thấy dữ liệu cũ đấy.
                context.updateProduct(productToEdit);

                Response response = new Response(MessageType.EDIT_PRODUCT_RESPONSE, "SUCCESS", "Cập nhật sản phẩm thành công!");
                conn.send(gson.toJson(response));

                System.out.println("-> [EditProduct] Thành công: ID " + productId);
            } else {
                sendError(conn, gson, "Lỗi khi cập nhật dữ liệu vào Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi xử lý: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.EDIT_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}