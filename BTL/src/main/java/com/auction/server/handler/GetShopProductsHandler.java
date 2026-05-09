package com.auction.server.handler;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandMap(value = MessageType.GET_SHOP_PRODUCTS_REQUEST)
public class GetShopProductsHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[GetShopProductsHandler] Client " + conn.getRemoteSocketAddress() + " hỏi sản phẩm shop...");

        try {
            // 1. Lấy sellerId từ request data
            // Client gửi kèm sellerId để biết lấy shop của ai
            String sellerId = (String) data.get("sellerId");

            // 2. Lấy filter status nếu có (null = lấy tất cả)
            String statusFilter = (String) data.get("status");

            // 3. Lấy toàn bộ rồi filter
            List<Product> result = context.getProductList().stream()
                    .filter(p -> {
                        // Lọc theo seller
                        boolean matchSeller = sellerId == null ||
                                (p.getOwner() != null && sellerId.equals(p.getOwner().getId()));

                        // Lọc theo status nếu có truyền vào
                        boolean matchStatus = statusFilter == null ||
                                p.getStatus().name().equals(statusFilter);

                        return matchSeller && matchStatus;
                    })
                    .collect(Collectors.toList());

            // 4. Đóng gói response
            Response response = new Response(
                    MessageType.GET_SHOP_PRODUCTS_RESPONSE,
                    "SUCCESS",
                    "Lấy danh sách sản phẩm shop thành công"
            );
            response.getData().put("products", result);

            conn.send(gson.toJson(response));
            System.out.println("[GetShopProductsHandler] Đã gửi " + result.size() + " sản phẩm!");

        } catch (Exception e) {
            System.err.println("[GetShopProductsHandler] Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}