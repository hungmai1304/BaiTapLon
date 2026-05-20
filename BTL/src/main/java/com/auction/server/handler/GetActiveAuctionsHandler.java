package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;

// 🚀 Đã gọi chuẩn từ MessageType
@CommandMap(value = MessageType.GET_ACTIVE_AUCTIONS_REQUEST)
public class GetActiveAuctionsHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // Móc danh sách Phiên đấu giá từ RAM ra
            List<Auction> activeAuctions = context.getActiveAuctions();

            // --- LOGIC LỌC DỮ LIỆU (Search Filtering) ---
            if (data != null) {
                String keyword = (String) data.get("keyword");
                String category = (String) data.get("category");
                String sellerName = (String) data.get("sellerName");

                activeAuctions = activeAuctions.stream()
                    .filter(a -> {
                        boolean match = true;
                        if (keyword != null && !keyword.isBlank()) {
                            match = match && a.getProduct() != null && a.getProduct().getName().toLowerCase().contains(keyword.toLowerCase());
                        }
                        if (category != null && !category.isBlank()) {
                            match = match && a.getProduct() != null && category.equalsIgnoreCase(a.getProduct().getCategory());
                        }
                        if (sellerName != null && !sellerName.isBlank()) {
                            match = match && a.getProduct() != null && a.getProduct().getOwner() != null && sellerName.equalsIgnoreCase(a.getProduct().getOwner().getUsername());
                        }
                        return match;
                    })
                    .toList();
            }
            // -------------------------------------------

            // Đóng gói trả về cho Client vừa gửi Request
            Response response = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Lấy danh sách đấu giá thành công!");
            response.getData().put("auctionList", activeAuctions);

            // Chỉ gửi trả lại cho ĐÚNG cái thằng vừa xin
            conn.send(gson.toJson(response));

            System.out.println("-> [GetActiveAuctions] Đã gửi " + activeAuctions.size() + " phiên đấu giá cho 1 Client (Sau khi lọc).");

        } catch (Exception e) {
            e.printStackTrace();
            Response errResponse = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "ERROR", "Lỗi server khi lấy danh sách: " + e.getMessage());
            conn.send(gson.toJson(errResponse));
        }
    }
}