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

@CommandMap(value = MessageType.GET_AUCTION_PRODUCT_REQUEST)
public class GetAuctionProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[GetAuctionProductHandler] Client " + conn.getRemoteSocketAddress() + " hỏi sản phẩm đang đấu giá...");

        try {
            // 1. Lấy danh sách sản phẩm từ context (sau này thay bằng DAO)
            // sau thêm databaseở dây 
            // ServerContext là nơi lưu trạng thái chung của server
            List<Product> allProducts = context.getProductList();

            // 2. Lọc chỉ lấy sản phẩm ON_AUCTION
            // Đây là lý do bước 1 thêm ProductStatus — để filter được
            List<Product> auctionProducts = allProducts.stream()
                    .filter(p -> p.getStatus() == ProductStatus.ON_AUCTION)
                    .collect(Collectors.toList());

            // 3. Đóng gói đúng cách — data chứa dữ liệu, message chứa thông báo
            Response response = new Response(
                    MessageType.GET_AUCTION_PRODUCT_RESPONSE,
                    "SUCCESS",
                    "Lấy sản phẩm đấu giá thành công"
            );
            response.getData().put("products", auctionProducts);

            conn.send(gson.toJson(response));
            System.out.println("[GetAuctionProductHandler] Đã gửi " + auctionProducts.size() + " sản phẩm đang đấu giá!");

        } catch (Exception e) {
            System.err.println("[GetAuctionProductHandler] Lỗi: " + e.getMessage());
            e.printStackTrace();
        }
    }
}