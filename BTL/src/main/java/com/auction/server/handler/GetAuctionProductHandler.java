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
            System.out.println("[Handler] Đang lấy danh sách sản phẩm đang đấu giá...");

            // Lấy danh sách từ RAM (ServerContext)
            List<Product> allProducts = context.getProductList();

            if (allProducts == null) {
                allProducts = new ArrayList<>();
            }

            // 1. Lọc các sản phẩm đang có trạng thái ON_AUCTION
            List<Product> auctionProducts = allProducts.stream()
                    .filter(p -> p != null && p.getStatus() != null && p.getStatus() == ProductStatus.ON_AUCTION)
                    .collect(Collectors.toList());

            // --- BƯỚC NÀY BỎ HẾT ---
            // ĐÉO CẦN đọc file Base64 nữa vì imagePath đã là link https://res.cloudinary...
            // -----------------------

            System.out.println("[Handler] Tìm thấy " + auctionProducts.size() + " sản phẩm đang đấu giá.");

            Response response = new Response(
                    MessageType.GET_AUCTION_PRODUCT_RESPONSE,
                    "SUCCESS",
                    "Lấy sản phẩm đấu giá thành công"
            );

            // Nhét danh sách vào data (imagePath lúc này chứa URL ảnh)
            response.getData().put("products", auctionProducts);

            // 2. Chuyển sang JSON và gửi đi
            String jsonResponse = gson.toJson(response);
            conn.send(jsonResponse);

            // --- CŨNG ĐÉO CẦN setBase64(null) nữa ---
            // Vì ngay từ đầu ta không hề nạp dữ liệu ảnh nặng vào Object

            System.out.println("[Handler] Đã gửi danh sách đấu giá. Client sẽ tự hiển thị ảnh qua URL.");

        } catch (Exception e) {
            System.err.println("[Handler ERROR]: " + e.toString());
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown Error";
            Response errorRes = new Response(MessageType.GET_AUCTION_PRODUCT_RESPONSE, "ERROR", errorMsg);
            conn.send(gson.toJson(errorRes));
        }
    }
}