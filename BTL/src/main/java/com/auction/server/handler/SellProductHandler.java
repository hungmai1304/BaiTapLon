package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
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
            // 1. Nhận ID của món hàng mà Shop muốn đưa lên sàn
            String productId = (String) data.get("productId");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Không tìm thấy mã sản phẩm!");
                return;
            }

            //  DATABASE : Gọi xuống DB để lấy thông tin món hàng trong "Kho"
            Product productToSell = ProductDao.getInstance().getProductById(productId); // Cần đảm bảo DAO có hàm này

            if (productToSell == null) {
                sendError(conn, gson, "Sản phẩm không tồn tại trong kho!");
                return;
            }

            // 2. Đổi trạng thái từ KHO (AVAILABLE) sang SÀN ĐẤU GIÁ (ON_AUCTION)
            productToSell.setStatus(ProductStatus.ON_AUCTION);

            //  DATABASE: Cập nhật trạng thái mới xuống Database
            boolean isUpdated = ProductDao.getInstance().updateProduct(productToSell); // Cần đảm bảo DAO có hàm update

            if (isUpdated) {
                // 3. Cập nhật vào RAM Server
                context.addProduct(productToSell); // Hoặc update nếu đã có trong list

                // Báo cho người bán biết là lên sàn thành công
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá!");
                conn.send(gson.toJson(response));

                System.out.println("✅ [SellProduct] Đã đưa SP lên sàn: " + productToSell.getName());

                // 4. 📢 YÊU CẦU MIRO: BROADCAST CHO CẢ LÀNG
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
        Response updateRes = new Response(MessageType.UPDATE_AUCTION_LIST_RESPONSE, "SUCCESS", "Sàn vừa có món mới!");
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