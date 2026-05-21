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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@CommandMap(value = MessageType.PLACE_BID_REQUEST)
public class PlaceBidHandler implements IMessageHandler {

    // QUẢN LÝ LUỒNG TẬP TRUNG: Sử dụng Thread Pool cố định thay vì tạo Thread vô tội vạ làm sập Server
    private static final ExecutorService botExecutor = Executors.newFixedThreadPool(4);

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("productId");

            Object bidAmountObj = data.get("bidAmount");
            if (bidAmountObj == null) {
                sendError(conn, gson, "Lỗi: Không tìm thấy mức giá đặt (bidAmount)!");
                return;
            }
            double bidAmount = ((Number) bidAmountObj).doubleValue();

            // BẢO MẬT TUYỆT ĐỐI: Lấy email trực tiếp từ session kết nối Socket
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Bạn chưa đăng nhập hoặc phiên làm việc hết hạn!");
                return;
            }

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Lỗi: Thiếu ID sản phẩm!");
                return;
            }

            // Kiểm tra trạng thái Blacklist của tài khoản
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null) {
                sendError(conn, gson, "Lỗi hệ thống: Không tìm thấy thông tin tài khoản!");
                return;
            }

            if ("BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                System.err.println("[PlaceBidHandler] Từ chối tài khoản Blacklist: " + userEmail);
                sendError(conn, gson, "Tài khoản của bạn đã bị khóa tham gia đấu giá (BLACKLIST)!");
                return;
            }

            Auction currentAuction = context.getAuctionByProductId(productId);
            if (currentAuction == null) {
                sendError(conn, gson, "Thất bại: Sản phẩm hiện không nằm trong phiên đấu giá nào!");
                return;
            }

            if ("PENDING".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Chưa đến giờ! Sản phẩm đang trong thời gian chờ mở sàn.");
                return;
            } else if ("COMPLETED".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Muộn rồi! Phiên đấu giá này đã kết thúc.");
                return;
            }

            // =========================================================================
            // ĐỒNG BỘ HÓA LUỒNG (THREAD-SAFE): Tránh Race Condition khi nhiều người cùng Bid một mili-giây
            // =========================================================================
            synchronized (currentAuction) {
                // Phải tính toán lại mức giá tối thiểu ngay bên trong khối synchronized
                double minRequiredPrice = (currentAuction.getHighestBidder() == null)
                        ? currentAuction.getStartPrice()
                        : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

                if (bidAmount < minRequiredPrice) {
                    sendError(conn, gson, "Giá đưa ra đã bị người khác dẫn trước! Vui lòng đặt tối thiểu: " + String.format("%,.0f", minRequiredPrice));
                    return;
                }

                // Kiểm tra ví tiền thực tế
                if (currentUser.getBalance() < bidAmount) {
                    sendError(conn, gson, "Số dư ví không đủ để thực hiện lượt đặt giá này!");
                    return;
                }

                // Tiến hành ghi nhận người dẫn đầu mới lên RAM
                User newLeader = new User();
                newLeader.setEmail(userEmail);
                newLeader.setUsername(currentUser.getUsername()); // Lấy tên hiển thị thực tế thay vì gán email

                currentAuction.setCurrentPrice(bidAmount);
                currentAuction.setHighestBidder(newLeader);
                currentAuction.setLeaderName(newLeader.getUsername());

                // Ghi nhận lịch sử giao dịch
                BidTransaction transaction = new BidTransaction();
                transaction.setId(currentAuction.getId());
                transaction.setBidder(newLeader);
                transaction.setBidAmount(bidAmount);
                transaction.setTimeCreated(LocalDateTime.now());

                if (currentAuction.getBiddingHistory() == null) {
                    currentAuction.setBiddingHistory(new ArrayList<>());
                }
                currentAuction.getBiddingHistory().add(transaction);

                context.updateAuction(currentAuction);
            }

            // Phát loa Real-time cho toàn bộ client nhận số liệu nhảy mới
            broadcastNewBid(context, gson, productId, bidAmount, userEmail);

            // Kích hoạt cuộc chiến Bot thông qua Executor điều phối luồng an toàn
            triggerBotWar(context, gson, productId, currentAuction);

            Response successRes = new Response(MessageType.PLACE_BID_RESPONSE, "SUCCESS", "Chúc mừng! Bạn đang là người dẫn đầu!");
            conn.send(gson.toJson(successRes));

            System.out.println("-> [Bid] " + userEmail + " đặt giá " + bidAmount + " cho SP: " + productId);

        } catch (Exception e) {
            System.err.println("[PlaceBidHandler] Lỗi hệ thống: " + e.getMessage());
            sendError(conn, gson, "Lỗi Server khi xử lý: " + e.getMessage());
        }
    }

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
    }

    // TỐI ƯU TOÀN DIỆN LUỒNG CHẠY BOT: Sử dụng Pool luồng tách biệt, chặn lặp vô hạn
    public static void triggerBotWar(ServerContext context, Gson gson, String productId, Auction currentAuction) {
        if (currentAuction.getRegisteredBots() == null || currentAuction.getRegisteredBots().isEmpty()) {
            return;
        }

        // Đẩy tác vụ chạy ngầm vào Executor quản lý tập trung, không sinh Thread rác bừa bãi
        botExecutor.submit(() -> {
            boolean keepFighting = true;
            int safetyCounter = 0; // Chống vòng lặp vô hạn chết người

            while (keepFighting && safetyCounter < 100) {
                keepFighting = false;
                safetyCounter++;

                // Đồng bộ hóa phiên đấu giá khi kiểm tra và nâng giá trong Bot
                synchronized (currentAuction) {
                    // Kiểm tra trạng thái phiên còn hợp lệ không trước khi Bot tự nâng
                    if (!"ACTIVE".equals(currentAuction.getStatus())) {
                        break;
                    }

                    for (AutoBidConfig bot : currentAuction.getRegisteredBots()) {
                        // Nếu Bot này đang dẫn đầu rồi thì bỏ qua không tự nâng giá của mình
                        if (currentAuction.getHighestBidder() != null && bot.getEmail().equals(currentAuction.getHighestBidder().getEmail())) {
                            continue;
                        }

                        double nextBotPrice = (currentAuction.getHighestBidder() == null)
                                ? currentAuction.getStartPrice()
                                : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

                        // Kiểm tra ví tiền thực tế của Bot từ DB
                        User botUserInfo = UserDao.getInstance().getUserByEmail(bot.getEmail());
                        if (botUserInfo == null || botUserInfo.getBalance() < nextBotPrice) {
                            continue;
                        }

                        // Kiểm tra trần giá của Bot cấu hình
                        if (nextBotPrice <= bot.getMaxPrice()) {
                            User botUser = new User();
                            botUser.setEmail(bot.getEmail());
                            botUser.setUsername(botUserInfo.getUsername());

                            currentAuction.setCurrentPrice(nextBotPrice);
                            currentAuction.setHighestBidder(botUser);
                            currentAuction.setLeaderName(botUser.getUsername());

                            BidTransaction transaction = new BidTransaction();
                            transaction.setId(currentAuction.getId());
                            transaction.setBidder(botUser);
                            transaction.setBidAmount(nextBotPrice);
                            transaction.setTimeCreated(LocalDateTime.now());

                            if (currentAuction.getBiddingHistory() == null) {
                                currentAuction.setBiddingHistory(new ArrayList<>());
                            }
                            currentAuction.getBiddingHistory().add(transaction);

                            context.updateAuction(currentAuction);

                            broadcastNewBid(context, gson, productId, nextBotPrice, bot.getEmail());
                            System.out.println("[BOT WAR] Bot " + bot.getEmail() + " đã kích giá lên: " + String.format("%,.0f", nextBotPrice));

                            keepFighting = true;
                            break; // Thoát ra ngoài vòng lặp for để cập nhật lại dữ liệu vòng lặp while mới
                        }
                    }
                }

                if (keepFighting) {
                    try {
                        Thread.sleep(500); // Nghỉ nửa giây tránh block nghẽn CPU
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        });
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.PLACE_BID_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}