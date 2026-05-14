package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.auction.server.service.FileService; // Nhớ check lại path này cho đúng (utils hay service)
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

            // --- 3. XỬ LÝ LÊN MÂY (CLOUDINARY) ---
            if (data.containsKey("imageBase64")) {
                String newImageBase64 = (String) data.get("imageBase64");

                if (newImageBase64 != null && !newImageBase64.isEmpty()) {
                    // Đẩy ảnh mới lên Cloudinary, nó sẽ overwrite ảnh cũ dựa trên productId
                    String newUrl = FileService.saveImage(newImageBase64, productId);

                    if (newUrl != null) {
                        productToEdit.setImagePath(newUrl); // Cập nhật link URL mới
                    }
                }
            }
            // ------------------------------------

            // 4. LƯU XUỐNG DATABASE
            boolean isUpdated = ProductDao.getInstance().editProduct(productToEdit);

            if (isUpdated) {
                // Đồng bộ lại vào RAM (ServerContext)
                // Đã có link URL mới nên các client sẽ thấy ảnh mới ngay lập tức
                context.updateProduct(productToEdit);

                Response response = new Response(MessageType.EDIT_PRODUCT_RESPONSE, "SUCCESS", "Cập nhật sản phẩm thành công!");
                conn.send(gson.toJson(response));

                System.out.println("-> [EditProduct] Thành công: ID " + productId + " (URL: " + productToEdit.getImagePath() + ")");
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