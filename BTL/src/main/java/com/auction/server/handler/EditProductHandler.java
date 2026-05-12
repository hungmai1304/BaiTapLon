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
            // 1. Lấy ID món hàng
            String productId = (String) data.get("productId");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Không tìm thấy mã sản phẩm để sửa!");
                return;
            }

            // Chọc xuống DB lấy sản phẩm gốc lên
            Product productToEdit = ProductDao.getInstance().getProductById(productId);

            if (productToEdit == null) {
                sendError(conn, gson, "Sản phẩm không tồn tại trong kho!");
                return;
            }

            // 2. CẬP NHẬT THÔNG TIN MỚI
            if (data.containsKey("name")) productToEdit.setName((String) data.get("name"));
            if (data.containsKey("category")) productToEdit.setCategory((String) data.get("category"));
            if (data.containsKey("description")) productToEdit.setDescription((String) data.get("description"));

            if (data.containsKey("startPrice")) {
                double newStartPrice = ((Number) data.get("startPrice")).doubleValue();
                productToEdit.setStartPrice(newStartPrice);

                // reset luôn giá hiện tại (currentPrice) nếu người bán đổi giá khởi điểm
                productToEdit.setCurrentPrice(newStartPrice);
            }

            if (data.containsKey("stepPrice")) {
                productToEdit.setStepPrice(((Number) data.get("stepPrice")).doubleValue());
            }

            // 3.DATABASE: Lưu thông tin mới xuống Database
            boolean isUpdated = ProductDao.getInstance().updateProduct(productToEdit);

            if (isUpdated) {
                // 4. Đồng bộ lại RAM của Server
                context.removeProduct(productId);    // Vứt bản ghi cũ đi
                context.addProduct(productToEdit);   // Nhét bản ghi mới cập nhật vào

                // Báo cho Shop biết là sửa thành công
                Response response = new Response(MessageType.EDIT_PRODUCT_RESPONSE, "SUCCESS", "Cập nhật sản phẩm thành công!");
                conn.send(gson.toJson(response));

                System.out.println("✅ [EditProduct] Đã sửa thông tin SP: " + productToEdit.getName());

                // 5.BROADCAST: Hét lên cho cả Server biết thông tin hàng vừa thay đổi!
                broadcastNewList(context, gson);

            } else {
                sendError(conn, gson, "Lỗi khi lưu thông tin mới vào Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void broadcastNewList(ServerContext context, Gson gson) {
        Response updateRes = new Response(MessageType.UPDATE_AUCTION_LIST_RESPONSE, "SUCCESS", "Thông tin sàn vừa được cập nhật!");
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