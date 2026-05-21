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

// Tiếp nhận đặt giá từ Client
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

            // BẢO MẬT: Lấy email trực tiếp từ session của kết nối thay vì tin tưởng Client gửi lên
            String userEmail = context.getUserByConn(conn);

            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Bạn chưa đăng nhập hoặc phiên làm việc không hợp lệ!");
                return;
            }

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Lỗi: Thiếu ID sản phẩm (productId)!");
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

            // 2. Tìm Phiên Đấu Giá chứa Sản phẩm này trên RAM
            Auction currentAuction = context.getAuctionByProductId(productId);

            if (currentAuction == null) {
                sendError(conn, gson, "Thất bại: Sản phẩm này hiện không nằm trong phiên đấu giá nào!");
                return;
            }

            // 3. Kiểm tra trạng thái Phiên Đấu giá
            if ("PENDING".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Chưa đến giờ! Sản phẩm đang trong thời gian quảng cáo chờ sàn mở.");
                return;
            } else if ("COMPLETED".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Muộn rồi! Phiên đấu giá này đã kết thúc.");
                return;
            }

            // 4. KIỂM TRA LUẬT ĐẤU GIÁ (Giá mới phải >= Giá hiện tại + Bước giá sản phẩm)
            double minRequiredPrice = (currentAuction.getHighestBidder() == null)
                    ? currentAuction.getStartPrice()
                    : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

            if (bidAmount < minRequiredPrice) {
                sendError(conn, gson, "Giá bạn đưa ra quá thấp! Phải lớn hơn hoặc bằng: " + String.format("%,.0f", minRequiredPrice));
                return;
            }

            // 5. KIỂM TRA VÍ TIỀN TRƯỚC KHI CHO ĐẶT GIÁ (Sử dụng lại biến currentUser đã khai báo ở trên)
            if (currentUser.getBalance() < bidAmount) {
                sendError(conn, gson, "Số dư ví không đủ (" + String.format("%,.0fđ", currentUser.getBalance()) + ")! Vui lòng nạp thêm tiền để tiếp tục đấu giá.");
                return;
            }

            // 6. Cập nhật dữ liệu vào Phiên Đấu Giá (RAM)
            User newLeader = new User();
            newLeader.setEmail(userEmail);
            newLeader.setUsername(userEmail);

            // Cập nhật thông tin phiên đấu giá (RAM)
            currentAuction.setCurrentPrice(bidAmount);
            currentAuction.setHighestBidder(newLeader);
            currentAuction.setLeaderName(newLeader.getUsername());

            // Ghi sổ lịch sử giao dịch bằng BidTransaction
            BidTransaction transaction = new BidTransaction();
            transaction.setId(String.valueOf(currentAuction.getId()));
            transaction.setBidder(newLeader);
            transaction.setBidAmount(bidAmount);
            transaction.setTimeCreated(LocalDateTime.now());

            // Kiểm tra an toàn bộ nhớ trước khi thêm vào danh sách lịch sử đặt giá
            if (currentAuction.getBiddingHistory() == null) {
                currentAuction.setBiddingHistory(new ArrayList<>());
            }
            currentAuction.getBiddingHistory().add(transaction);

            // Lưu dữ liệu cập nhật mới vào bộ nhớ RAM hệ thống
            context.updateAuction(currentAuction);

            // 7. PHÁT LOA CHO TOÀN BỘ SÀN ĐỂ NHẢY SỐ REAL-TIME (Gọi hàm static nội bộ)
            broadcastNewBid(context, gson, productId, bidAmount, userEmail);

            // 8. KÍCH HOẠT BOT TỰ ĐỘNG TRANH CHẤP GIÁ (Gọi hàm static nội bộ)
            triggerAutoBidding(context, gson, productId, currentAuction);

            // 9. Gửi phản hồi riêng xác nhận thành công cho người vừa thao tác đặt giá
            Response successRes = new Response(MessageType.PLACE_BID_RESPONSE, "SUCCESS", "Chúc mừng! Bạn đang là người dẫn đầu phiên!");
            conn.send(gson.toJson(successRes));

            System.out.println("-> [Bid] " + userEmail + " vừa ra giá " + bidAmount + " cho SP: " + productId);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi Server khi xử lý đấu giá: " + e.getMessage());
        }
    }

    /**
     * ĐÃ CHUYỂN THÀNH PUBLIC STATIC: Hàm phát loa thông báo nhảy số thời gian thực
     */
    public static void broadcastNewBid(ServerContext context, Gson gson, String productId, double newPrice, String leaderName) {
        Response broadcastRes = new Response(MessageType.BROADCAST_NEW_BID, "SUCCESS", "Có mức giá mới!");
        broadcastRes.getData().put("newPrice", newPrice);
        broadcastRes.getData().put("leaderName", leaderName);
        broadcastRes.getData().put("productId", productId);

        String message = gson.toJson(broadcastRes);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
        System.out.println("   -> [Broadcast Bid] Đã thông báo giá mới (" + newPrice + ") cho toàn Server.");
    }

    /**
     * ĐÃ CHUYỂN THÀNH PUBLIC STATIC: Hàm kích hoạt cuộc chiến giữa các Bot tự động nâng giá
     */
    public static void triggerAutoBidding(ServerContext context, Gson gson, String productId, Auction currentAuction) {
        if (currentAuction.getRegisteredBots() == null || currentAuction.getRegisteredBots().isEmpty()) {
            return;
        }

        boolean keepFighting = true;

        // Vòng lặp liên tục cho đến khi không còn Bot nào đủ điều kiện hoặc dám lên giá nữa
        while (keepFighting) {
            keepFighting = false;

            for (AutoBidConfig bot : currentAuction.getRegisteredBots()) {
                // Nếu Bot hiện tại đang giữ vị trí dẫn đầu rồi thì bỏ qua không cần nâng giá tiếp
                if (currentAuction.getHighestBidder() != null && bot.getEmail().equals(currentAuction.getHighestBidder().getEmail())) {
                    continue;
                }

                // Tính toán mức giá tiếp theo Bot cần phải trả
                double nextBotPrice;
                if (currentAuction.getHighestBidder() == null) {
                    nextBotPrice = currentAuction.getStartPrice();
                } else {
                    // Logic chuẩn xác: Phải cộng thêm bước giá (StepPrice) quy định của chính Phiên Đấu Giá
                    nextBotPrice = currentAuction.getCurrentPrice() + currentAuction.getStepPrice();
                }

                // Nếu giá tính toán vẫn nằm trong hạn mức tài chính tối đa (MaxPrice) mà chủ Bot thiết lập
                if (nextBotPrice <= bot.getMaxPrice()) {
                    User botUser = new User();
                    botUser.setEmail(bot.getEmail());
                    botUser.setUsername(bot.getEmail());

                    currentAuction.setCurrentPrice(nextBotPrice);
                    currentAuction.setHighestBidder(botUser);
                    currentAuction.setLeaderName(botUser.getUsername());

                    // Lưu vết giao dịch của Bot vào lịch sử
                    BidTransaction transaction = new BidTransaction();
                    transaction.setId(String.valueOf(currentAuction.getId()));
                    transaction.setBidder(botUser);
                    transaction.setBidAmount(nextBotPrice);
                    transaction.setTimeCreated(LocalDateTime.now());

                    if (currentAuction.getBiddingHistory() == null) {
                        currentAuction.setBiddingHistory(new ArrayList<>());
                    }
                    currentAuction.getBiddingHistory().add(transaction);

                    // Đồng bộ RAM
                    context.updateAuction(currentAuction);

                    // Phát tin nhảy số cho toàn bộ Server hiển thị giá của Bot vừa đặt (Hàm static gọi hàm static)
                    broadcastNewBid(context, gson, productId, nextBotPrice, botUser.getEmail());
                    System.out.println(" [BOT WAR] Bot " + bot.getEmail() + " đã trả giá lên: " + nextBotPrice);

                    keepFighting = true; // Kích hoạt tiếp vòng lặp cho các Bot khác vào so kè

                    try {
                        Thread.sleep(500); // Tránh bot spam quá nhanh gây lag nghẽn mạng luồng socket
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
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