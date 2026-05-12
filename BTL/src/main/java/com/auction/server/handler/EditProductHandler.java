package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

@CommandMap(value = MessageType.EDIT_PRODUCT_REQUEST)
public class EditProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. Lấy ID từ "id" (khớp với RequestSender ở Client)
            String productId = (String) data.get("id");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Không tìm thấy mã sản phẩm để sửa!");
                return;
            }

            // Lấy sản phẩm hiện tại từ Database để có đủ thông tin gốc (như Owner, TimeCreated)
            Product productToEdit = ProductDao.getInstance().getProductById(productId);

            if (productToEdit == null) {
                sendError(conn, gson, "Sản phẩm không tồn tại trên hệ thống!");
                return;
            }

            // 2. CẬP NHẬT THÔNG TIN MỚI (Ghi đè các trường từ JSON gửi lên)
            if (data.containsKey("name")) productToEdit.setName((String) data.get("name"));
            if (data.containsKey("category")) productToEdit.setCategory((String) data.get("category"));
            if (data.containsKey("description")) productToEdit.setDescription((String) data.get("description"));

            // Xử lý giá (Dùng Number để tránh lỗi ép kiểu Double/Integer từ Gson)
            if (data.containsKey("startPrice")) {
                double newPrice = ((Number) data.get("startPrice")).doubleValue();
                productToEdit.setStartPrice(newPrice);
                productToEdit.setCurrentPrice(newPrice); // Reset giá hiện tại bằng giá khởi điểm mới
            }

            if (data.containsKey("stepPrice")) {
                productToEdit.setStepPrice(((Number) data.get("stepPrice")).doubleValue());
            }

            // 3. LƯU XUỐNG DATABASE
            boolean isUpdated = ProductDao.getInstance().updateProduct(productToEdit);

            if (isUpdated) {
                // 4. ĐỒNG BỘ RAM (Thay vì xóa rồi thêm, ta dùng hàm update chuẩn)
                // Ông hãy nhớ thêm hàm updateProduct này vào ServerContext như tôi hướng dẫn ở trên
                context.updateProduct(productToEdit);

                // 5. PHẢN HỒI CHO SHOP
                Response response = new Response(MessageType.EDIT_PRODUCT_RESPONSE, "SUCCESS", "Cập nhật sản phẩm thành công!");
                conn.send(gson.toJson(response));

                System.out.println("-> [EditProduct] Thành công: ID " + productId);

                // 6. BROADCAST (Hét lên cho cả Server cập nhật lại danh sách hiển thị)
                broadcastNewList(context, gson);

            } else {
                sendError(conn, gson, "Lỗi khi cập nhật dữ liệu vào Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi xử lý: " + e.getMessage());
        }
    }

    private void broadcastNewList(ServerContext context, Gson gson) {
        Response updateRes = new Response(MessageType.UPDATE_AUCTION_LIST_RESPONSE, "SUCCESS", "Danh sách sản phẩm vừa được cập nhật.");
        updateRes.getData().put("productList", context.getProductList());
        String message = gson.toJson(updateRes);

        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.EDIT_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}