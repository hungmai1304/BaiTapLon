package com.auction.server.handler.bidding;

import com.auction.protocol.MessageType;
import com.auction.common.model.auction.Auction;
import com.auction.common.model.auction.BidTransaction;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import java.time.LocalDateTime;
import java.util.ArrayList;

import java.util.Map;
// tiếp nhận đặt giá
@CommandMap(value = MessageType.PLACE_BID_REQUEST)
public class PlaceBidHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("productId");
            
            // Xử lý an toàn: Lấy bidAmount với kiểm tra null để tránh NullPointerException
            Object bidAmountObj = data.get("bidAmount");
            if (bidAmountObj == null) {
                sendError(conn, gson, "Lỗi: Không tìm thấy mức giá đặt (bidAmount)!");
                return;
            }
            double bidAmount = ((Number) bidAmountObj).doubleValue();

            // BẢO MẬT: Lấy email trực tiếp từ session của kết nối thay vì tin tưởng Client
            String userEmail = context.getUserByConn(conn);

            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Bạn chưa đăng nhập hoặc phiên làm việc không hợp lệ!");
                return;
            }

            if (productId == null) {
                sendError(conn, gson, "Lỗi: Thiếu ID sản phẩm (productId)!");
                return;
            }

            // 2. Tìm Phiên Đấu Giá chứa Sản phẩm này trên RAM
            Auction currentAuction = context.getAuctionByProductId(productId);

            if (currentAuction == null) {
                sendError(conn, gson, "Thất bại: Sản phẩm này hiện không nằm trong phiên đấu giá nào!");
                return;
            }

            // 3. KIỂM TRA & CẬP NHẬT TRẠNG THÁI (Thread-safe trên RAM)
            // CHIẾN THUẬT HOLD/REFUND: Tạm giữ tiền của người đặt giá mới và hoàn trả người cũ
            synchronized (currentAuction) {
                // Kiểm tra trạng thái Phiên đấu giá
                if ("PENDING".equals(currentAuction.getStatus())) {
                    sendError(conn, gson, "Chưa đến giờ! Sản phẩm đang trong 30 phút quảng cáo.");
                    return;
                } else if ("COMPLETED".equals(currentAuction.getStatus())) {
                    sendError(conn, gson, "Muộn rồi! Phiên đấu giá này đã kết thúc.");
                    return;
                }

                // 4. KIỂM TRA LUẬT ĐẤU GIÁ (Giá mới phải >= Giá hiện tại + Bước giá)
                double minRequiredPrice = (currentAuction.getHighestBidder() == null)
                        ? currentAuction.getStartPrice()
                        : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

                if (bidAmount < minRequiredPrice) {
                    sendError(conn, gson, "Giá bạn đưa ra quá thấp! Phải lớn hơn hoặc bằng: " + String.format("%,.0fđ", minRequiredPrice));
                    return;
                }

                // 5. CHIẾN THUẬT HOLD/REFUND: Tạm giữ tiền của người đặt giá mới
                // [HOLD] Thử trừ tiền người đặt giá mới (Atomic SQL check)
                boolean isHoldSuccess = UserDao.getInstance().withdrawMoney(userEmail, bidAmount);
                if (!isHoldSuccess) {
                    sendError(conn, gson, "Số dư ví không đủ để thực hiện lượt đặt giá " + String.format("%,.0fđ", bidAmount) + "!");
                    return;
                }

                // [REFUND] Nếu có người dẫn đầu trước đó -> Hoàn tiền cho họ
                User previousLeader = currentAuction.getHighestBidder();
                double previousBid = currentAuction.getCurrentPrice();
                if (previousLeader != null) {
                    UserDao.getInstance().depositMoney(previousLeader.getEmail(), previousBid);
                    System.out.println("   [Refund] Đã hoàn trả " + String.format("%,.0fđ", previousBid) + " cho người cũ: " + previousLeader.getEmail());
                }

                // 6. Cập nhật dữ liệu vào Phiên Đấu Giá (RAM)
                User newLeader = new User();
                newLeader.setEmail(userEmail);
                newLeader.setUsername(userEmail);

                currentAuction.setCurrentPrice(bidAmount);
                currentAuction.setHighestBidder(newLeader);
                currentAuction.setLeaderName(newLeader.getUsername());

                // Ghi sổ lịch sử bằng BidTransaction
                BidTransaction transaction = new BidTransaction();
                transaction.setId(String.valueOf(currentAuction.getId()));
                transaction.setBidder(newLeader);
                transaction.setBidAmount(bidAmount);
                transaction.setTimeCreated(LocalDateTime.now());

                if (currentAuction.getBiddingHistory() == null) {
                    currentAuction.setBiddingHistory(new ArrayList<>());
                }
                currentAuction.getBiddingHistory().add(transaction);

                // Lưu lại vào RAM
                context.updateAuction(currentAuction);
            }

            // 7. PHÁT LOA CHO CẢ SÀN THEO ĐÚNG GIAO KÈO
            broadcastNewBid(context, gson, productId, bidAmount, userEmail);

            // 8. Gửi phản hồi riêng cho người vừa đấu giá thành công
            Response successRes = new Response(MessageType.PLACE_BID_RESPONSE, "SUCCESS", "Chúc mừng! Bạn đang là người dẫn đầu!");
            conn.send(gson.toJson(successRes));

            System.out.println("-> [Bid] " + userEmail + " vừa ra giá " + bidAmount + " cho SP: " + productId);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi Server khi xử lý đấu giá: " + e.getMessage());
        }
    }

    // HÀM PHÁT LOA NHẢY SỐ

    private void broadcastNewBid(ServerContext context, Gson gson, String productId, double newPrice, String leaderName) {

        Response broadcastRes = new Response(MessageType.BROADCAST_NEW_BID, "SUCCESS", "Có mức giá mới!");

        broadcastRes.getData().put("newPrice", newPrice);
        broadcastRes.getData().put("leaderName", leaderName);

        // Tặng kèm productId để Client biết màn hình nào cần nhảy số
        broadcastRes.getData().put("productId", productId);

        String message = gson.toJson(broadcastRes);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
        System.out.println("   -> [Broadcast Bid] Đã thông báo giá mới (" + newPrice + ") cho toàn Server.");
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.PLACE_BID_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}