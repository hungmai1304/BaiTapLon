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
import java.util.List;

@CommandMap(value = MessageType.SELL_PRODUCT_REQUEST)
public class SellProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("id");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Không tìm thấy mã sản phẩm để lên sàn!");
                return;
            }

            // 1. Cập nhật trạng thái trong Database
            boolean isSold = ProductDao.getInstance().sellProduct(productId);

            if (isSold) {
                // 2. Cập nhật RAM: Lấy bản mới nhất từ DB (có start_time, end_time) nhét vào Context
                Product updatedProduct = ProductDao.getInstance().getProductById(productId);
                if (updatedProduct != null) {
                    context.updateProduct(updatedProduct);
                }

                // 3. Phản hồi cho thằng Seller
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá!");
                conn.send(gson.toJson(response));

                System.out.println("-> [SellProduct] Thành công: ID " + productId);

                // 4. Phát loa thông báo cho tất cả mọi người
                broadcastNewList(context, gson);

            } else {
                sendError(conn, gson, "Lỗi khi cập nhật trạng thái lên Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void broadcastNewList(ServerContext context, Gson gson) {
        // Lấy danh sách sản phẩm từ RAM
        List<Product> listToSend = context.getProductList();

        // --- BƯỚC QUAN TRỌNG: Bơm ảnh vào để mọi người thấy hình ---
        for (Product p : listToSend) {
            if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
                p.setImageBase64(FileService.readImageAsBase64(p.getImagePath()));
            }
        }

        Response updateRes = new Response(MessageType.UPDATE_AUCTION_LIST_RESPONSE, "SUCCESS", "Sàn vừa có món mới!");
        updateRes.getData().put("productList", listToSend);

        String message = gson.toJson(updateRes);

        // Gửi cho tất cả mọi người đang online
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }

        // --- BƯỚC QUAN TRỌNG: Xóa Base64 ngay sau khi gửi để giải phóng RAM ---
        for (Product p : listToSend) {
            p.setImageBase64(null);
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}