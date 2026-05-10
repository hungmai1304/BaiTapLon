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
            System.out.println("[Handler] Đang xử lý lấy sản phẩm...");
            List<Product> allProducts = context.getProductList();

            if (allProducts == null) {
                System.out.println("[Handler] Cảnh báo: allProducts bị null");
                allProducts = new ArrayList<>();
            }

            // Kiểm tra trạng thái từng sản phẩm để tránh NullPointerException âm thầm
            List<Product> auctionProducts = allProducts.stream()
                    .filter(p -> p != null && p.getStatus() != null && p.getStatus() == ProductStatus.ON_AUCTION)
                    .collect(Collectors.toList());

            System.out.println("[Handler] Tìm thấy " + auctionProducts.size() + " sản phẩm ON_AUCTION.");

            Response response = new Response(
                    MessageType.GET_AUCTION_PRODUCT_RESPONSE,
                    "SUCCESS",
                    "Lấy sản phẩm thành công"
            );

            response.getData().put("products", auctionProducts);

            // Bước này cực kỳ dễ lỗi nếu Product chứa object phức tạp
            String jsonResponse;
            try {
                jsonResponse = gson.toJson(response);
            } catch (Exception jsonEx) {
                System.err.println("[Handler] Lỗi GSON không thể chuyển Product sang JSON: " + jsonEx.getMessage());
                throw jsonEx;
            }

            conn.send(jsonResponse);
            System.out.println("[Handler] Đã gửi JSON thành công!");

        } catch (Exception e) {
            System.err.println("[Handler ERROR] Tại dòng xử lý: " + e.toString());
            e.printStackTrace(); // In ra lỗi màu đỏ trên Server Console

            // Gửi báo lỗi kèm nội dung lỗi để Client biết
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown Error";
            conn.send("{\"type\":\"" + MessageType.GET_AUCTION_PRODUCT_RESPONSE + "\", \"status\":\"ERROR\", \"message\":\"" + errorMsg + "\"}");
        }
    }
}