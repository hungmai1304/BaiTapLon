package com.auction.server.handler;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandMap(value = MessageType.GET_AUCTION_PRODUCT_REQUEST)
public class GetAuctionProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            List<Product> allProducts = context.getProductList();

            // Khởi tạo list trống nếu context null để tránh crash
            if (allProducts == null) {
                allProducts = new ArrayList<>();
            }

            List<Product> auctionProducts = allProducts.stream()
                    .filter(p -> p != null && p.getStatus() == ProductStatus.ON_AUCTION)
                    .collect(Collectors.toList());

            // Đảm bảo truyền MessageType kiểu String
            Response response = new Response(
                    MessageType.GET_AUCTION_PRODUCT_RESPONSE,
                    "SUCCESS",
                    "Lấy sản phẩm thành công"
            );

            // Response đã khởi tạo Map data trong constructor
            response.getData().put("products", auctionProducts);

            String jsonResponse = gson.toJson(response);
            conn.send(jsonResponse);

            System.out.println("[Handler] Sent " + auctionProducts.size() + " products to client.");

        } catch (Exception e) {
            System.err.println("[Handler Error] GET_AUCTION_PRODUCT: " + e.getMessage());
            e.printStackTrace();
            conn.send("{\"type\":\"" + MessageType.GET_AUCTION_PRODUCT_RESPONSE + "\", \"status\":\"ERROR\"}");
        }
    }
}