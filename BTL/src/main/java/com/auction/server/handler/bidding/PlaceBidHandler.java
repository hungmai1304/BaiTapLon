package com.auction.server.handler.bidding;

import com.auction.common.model.auction.AutoBidConfig;
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

            // =========================================================================
            // KIỂM TRA TRẠNG THÁI BLACKLIST TRƯỚC KHI CHO PHÉP ĐẶT GIÁ
            // =========================================================================
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null) {
                sendError(conn, gson, "Lỗi hệ thống: Không tìm thấy thông tin tài khoản của bạn!");
                return;
            }

            // Nếu trạng thái là BLACKLIST thì chặn ngay lập tức
            if ("BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                System.err.println("[PlaceBidHandler] Từ chối: Tài khoản thuộc danh sách đen " + userEmail + " cố gắng đặt giá!");
                sendError(conn, gson, "Tài khoản của bạn đã bị đưa vào danh sách đen (BLACKLIST). Bạn không có quyền tham gia đấu giá!");
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

            // 3. Kiểm tra trạng thái Phiên đấu giá
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
                sendError(conn, gson, "Giá bạn đưa ra quá thấp! Phải lớn hơn hoặc bằng: " + minRequiredPrice);
                return;
            }

            //  4.5 KIỂM TRA VÍ TIỀN TRƯỚC KHI CHO ĐẶT GIÁ
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null) {
                sendError(conn, gson, "Lỗi hệ thống: Không tìm thấy thông tin ví của bạn!");
                return;
            }

            // Kiểm tra xem tiền trong ví có đủ để trả mức giá vừa nhập không
            if (currentUser.getBalance() < bidAmount) {
                sendError(conn, gson, "Số dư ví không đủ (" + String.format("%,.0fđ", currentUser.getBalance()) + ")! Vui lòng nạp thêm tiền để đấu giá.");
                return;
            }

            // 5. Cập nhật dữ liệu vào Phiên Đấu Giá (RAM)
            User newLeader = new User();
            newLeader.setEmail(userEmail);
            newLeader.setUsername(userEmail);

            //Cập nhật thông tin phiên đấu giá (RAM)
            currentAuction.setCurrentPrice(bidAmount);
            currentAuction.setHighestBidder(newLeader);
            currentAuction.setLeaderName(newLeader.getUsername());

            // Ghi sổ lịch sử bằng BidTransaction
            BidTransaction transaction = new BidTransaction();
            transaction.setId(String.valueOf(currentAuction.getId()));
            transaction.setBidder(newLeader);
            transaction.setBidAmount(bidAmount);
            transaction.setTimeCreated(LocalDateTime.now());

            // Kiểm tra an toàn trước khi add vào list
            if (currentAuction.getBiddingHistory() == null) {
                currentAuction.setBiddingHistory(new ArrayList<>());
            }
            currentAuction.getBiddingHistory().add(transaction);

            // Lưu lại vào RAM
            context.updateAuction(currentAuction);

            // 6. PHÁT LOA CHO CẢ SÀN THEO ĐÚNG GIAO KÈO
            broadcastNewBid(context, gson, productId, bidAmount, userEmail);

            //  Người thật vừa đấu xong, gọi Bot dậy xem có đấu giá được không
            triggerAutoBidding(context, gson, productId, currentAuction);

            // 7. Gửi phản hồi riêng cho người vừa đấu giá thành công
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

    // HÀM KÍCH HOẠT BOT TỰ ĐỘNG NHẢY SỐ
    private void triggerAutoBidding(ServerContext context, Gson gson, String productId, Auction currentAuction) {
        if (currentAuction.getRegisteredBots() == null || currentAuction.getRegisteredBots().isEmpty()) {
            return;
        }

        boolean keepFighting = true;

        // Vòng lặp Đấu cho đến khi không con Bot nào dám lên giá nữa
        while (keepFighting) {
            keepFighting = false;

            for (AutoBidConfig bot : currentAuction.getRegisteredBots()) {

                if (currentAuction.getHighestBidder() != null && bot.getEmail().equals(currentAuction.getHighestBidder().getEmail())) {
                    continue;
                }

                // 2. Xử lý chưa ai đặt giá
                double nextBotPrice;
                if (currentAuction.getHighestBidder() == null) {
                    nextBotPrice = currentAuction.getStartPrice(); // Nếu chưa ai đấu, Bot luôn giá khởi điểm
                } else {
                    nextBotPrice = currentAuction.getCurrentPrice() + bot.getStepPrice();
                }

                // 3. Nếu giá vẫn nằm trong khả năng tài chính của Bot này
                if (nextBotPrice <= bot.getMaxPrice()) {

                    // Cập nhật giá mới
                    User botUser = new User();
                    botUser.setEmail(bot.getEmail());
                    botUser.setUsername(bot.getEmail());

                    currentAuction.setCurrentPrice(nextBotPrice);
                    currentAuction.setHighestBidder(botUser);
                    currentAuction.setLeaderName(botUser.getUsername());

                    // Lưu lịch sử
                    BidTransaction transaction = new BidTransaction();
                    transaction.setId(String.valueOf(currentAuction.getId()));
                    transaction.setBidder(botUser);
                    transaction.setBidAmount(nextBotPrice);
                    transaction.setTimeCreated(java.time.LocalDateTime.now());

                    if (currentAuction.getBiddingHistory() == null) {
                        currentAuction.setBiddingHistory(new java.util.ArrayList<>());
                    }
                    currentAuction.getBiddingHistory().add(transaction);

                    // Cập nhật RAM
                    context.updateAuction(currentAuction);

                    broadcastNewBid(context, gson, productId, nextBotPrice, botUser.getEmail());
                    System.out.println(" [BOT WAR] Bot " + bot.getEmail() + " đè giá lên: " + nextBotPrice);

                    keepFighting = true; //  đặt lại để bot khác đấu lại

                    //
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {}

                    break;
                }
            }
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.PLACE_BID_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}