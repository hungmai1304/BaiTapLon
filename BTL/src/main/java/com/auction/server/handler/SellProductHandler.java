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

@CommandMap(value = MessageType.SELL_PRODUCT_REQUEST)
public class SellProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. Nhận ID chuẩn từ Client gửi lên (Đã sửa thành "id")
            String productId = (String) data.get("id");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Không tìm thấy mã sản phẩm để lên sàn!");
                return;
            }

            // 2. DATABASE: Chọc thẳng xuống DB để cập nhật trạng thái ON_AUCTION, start_time, end_time
            boolean isSold = ProductDao.getInstance().sellProduct(productId);

            if (isSold) {
                // 3. ĐỒNG BỘ RAM: Móc món hàng mới nhất từ DB lên để nhét vào RAM
                // (Phải có bước này thì lúc Broadcast danh sách nó mới mang dữ liệu mới nhất đi)
//                Product updatedProduct = ProductDao.getInstance().getProductById(productId);
//                if (updatedProduct != null) {
//                    // Update lại món hàng đó trong RAM
//                    context.updateProduct(updatedProduct);
//                }

                // 4. Phản hồi cho thằng Shop vừa bấm nút Sell
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá!");
                conn.send(gson.toJson(response));
                System.out.println("✅ [SellProduct] Đã đưa SP có ID " + productId + " lên sàn!");

                // 5. 📢 BẬT LOA PHÁT THANH: Báo cho toàn bộ người dùng đang online biết sàn có đồ mới!
                broadcastNewList(context, gson);

            } else {
                sendError(conn, gson, "Lỗi khi cập nhật trạng thái lên Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // Cái loa phát thanh
    private void broadcastNewList(ServerContext context, Gson gson) {
        Response updateRes = new Response(MessageType.UPDATE_AUCTION_LIST_RESPONSE, "SUCCESS", "Sàn vừa có món mới!");
        // Lưu ý: Key "productList" phải khớp với bên Client
        updateRes.getData().put("productList", context.getProductList());
        String message = gson.toJson(updateRes);

        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}