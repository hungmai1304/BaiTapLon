package com.auction.server.handler.bidding;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.auction.AutoBidConfig;
import com.auction.common.model.auction.BidTransaction;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.auction.server.service.AuctionManager;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@CommandMap(value = MessageType.PLACE_BID_REQUEST)
public class PlaceBidHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(PlaceBidHandler.class.getName());

    private static final ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(4);

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("productId");
            double bidAmount = ((Number) data.get("bidAmount")).doubleValue();

            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Chưa đăng nhập!");
                return;
            }

            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null || "BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                sendError(conn, gson, "Tài khoản không hợp lệ!");
                return;
            }

            Auction currentAuction = context.getAuctionByProductId(productId);
            if (currentAuction == null || !"ACTIVE".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Phiên đấu giá không sẵn sàng!");
                return;
            }

            User previousLeader = null;
            double previousBid = 0;
            String safeUserName = (currentUser.getUsername() != null && !currentUser.getUsername().trim().isEmpty())
                    ? currentUser.getUsername() : userEmail;

            synchronized (currentAuction) {
                if (currentAuction.getHighestBidder() != null
                        && currentAuction.getHighestBidder().getEmail().equalsIgnoreCase(userEmail)) {
                    sendError(conn, gson, "Bạn đang dẫn đầu, không cần đặt thêm!");
                    return;
                }

                double minRequiredPrice = (currentAuction.getHighestBidder() == null)
                        ? currentAuction.getStartPrice()
                        : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

                if (bidAmount < minRequiredPrice) {
                    sendError(conn, gson, "Giá tối thiểu cần đặt: " + String.format("%,.0f", minRequiredPrice));
                    return;
                }

                boolean isHoldSuccess = UserDao.getInstance().withdrawMoney(userEmail, bidAmount);
                if (!isHoldSuccess) {
                    sendError(conn, gson, "Số dư ví không đủ!");
                    return;
                }

                previousLeader = currentAuction.getHighestBidder();
                previousBid = currentAuction.getCurrentPrice();

                User newLeader = new User();
                newLeader.setEmail(userEmail);
                newLeader.setUsername(safeUserName);

                currentAuction.setCurrentPrice(bidAmount);
                currentAuction.setHighestBidder(newLeader);
                currentAuction.setLeaderName(safeUserName);

                // =========================================================================
                // ĐÃ SỬA CẤU TRÚC: Khởi tạo Lịch sử giao dịch bằng constructor rỗng
                // và nạp dữ liệu qua hàm set có sẵn để không làm hỏng file cũ của bạn.
                // =========================================================================
                BidTransaction transaction = new BidTransaction();
                transaction.setBidder(newLeader);
                transaction.setBidAmount(bidAmount);

                // Nếu lớp BaseEntity của bạn có hàm bổ trợ set thời gian tạo/Id tự động, hãy bổ sung ở đây:
                // transaction.setCreatedAt(LocalDateTime.now());

                if (currentAuction.getBiddingHistory() == null) {
                    currentAuction.setBiddingHistory(new ArrayList<>());
                }
                currentAuction.getBiddingHistory().add(transaction);
                // =========================================================================

                context.updateAuction(currentAuction);
                updateClientBalance(context, gson, userEmail);
            }

            if (previousLeader != null) {
                final User finalPrevLeader = previousLeader;
                final double finalPrevBid = previousBid;
                botScheduler.submit(() -> {
                    UserDao.getInstance().depositMoney(finalPrevLeader.getEmail(), finalPrevBid);
                    updateClientBalance(context, gson, finalPrevLeader.getEmail());
                });
            }

            broadcastNewBid(context, gson, productId, bidAmount, safeUserName);

            // Kích hoạt trận chiến Bot phản công
            triggerBotWar(context, gson, productId, currentAuction);

            Response successRes = new Response(MessageType.PLACE_BID_RESPONSE, "SUCCESS", "Bạn đang dẫn đầu!");
            conn.send(gson.toJson(successRes));

        } catch (Exception e) {
            sendError(conn, gson, "Lỗi Server: " + e.getMessage());
        }
    }

    public static void triggerBotWar(ServerContext context, Gson gson, String productId, Auction currentAuction) {
        String auctionId = currentAuction.getId();

        // 1. KIỂM TRA NGAY TỪ ĐẦU: Nếu hàng đợi đang bị đóng băng 10s thì CHẶN LUÔN không cho chạy tiếp
        if (AuctionManager.getInstance().isBotFrozen(auctionId)) {
            return;
        }

        Queue<AutoBidConfig> queue = AuctionManager.getInstance().getBotQueue(auctionId);
        if (queue == null || queue.isEmpty()) return;

        botScheduler.submit(() -> {
            synchronized (currentAuction) {
                // Kiểm tra lại một lần nữa trong khối đồng bộ để tránh Race Condition
                if (!"ACTIVE".equals(currentAuction.getStatus()) || AuctionManager.getInstance().isBotFrozen(auctionId)) {
                    return;
                }

                while (!queue.isEmpty()) {
                    AutoBidConfig currentBot = queue.peek();
                    if (currentBot == null) return;

                    // TH 1: Bot đầu hàng đang dẫn đầu -> đẩy xuống cuối hàng, check con tiếp theo luôn
                    if (currentAuction.getHighestBidder() != null
                            && currentBot.getEmail().equals(currentAuction.getHighestBidder().getEmail())) {
                        queue.poll();
                        queue.add(currentBot);
                        continue;
                    }

                    double nextBotPrice = (currentAuction.getHighestBidder() == null)
                            ? currentAuction.getStartPrice()
                            : (currentAuction.getCurrentPrice() + currentBot.getStepPrice());

                    // TH 2: Vượt quá giá trần -> Cút lập tức, check con tiếp theo
                    if (nextBotPrice > currentBot.getMaxPrice()) {
                        queue.poll();
                        LOGGER.info("[BOT OUT] Bot " + currentBot.getEmail() + " vượt trần, bị loại.");
                        continue;
                    }

                    // TH 3: Kiểm tra ví tiền trong DB -> Hết tiền cút lập tức, check con tiếp theo
                    User botUserInfo = UserDao.getInstance().getUserByEmail(currentBot.getEmail());
                    if (botUserInfo == null || botUserInfo.getBalance() < nextBotPrice) {
                        queue.poll();
                        LOGGER.info("[BOT OUT] Bot " + currentBot.getEmail() + " hết tiền, bị loại.");
                        continue;
                    }

                    // =========================================================================
                    // NẾU VƯỢT QUA CÁC ĐIỀU KIỆN TRÊN -> THỰC HIỆN ĐẶT BID THÀNH CÔNG
                    // =========================================================================
                    boolean botHold = UserDao.getInstance().withdrawMoney(currentBot.getEmail(), nextBotPrice);
                    if (!botHold) {
                        queue.poll();
                        continue;
                    }

                    // BẬT CỜ ĐÓNG BĂNG NGAY LẬP TỨC để block tất cả các luồng trigger khác gọi tới
                    AuctionManager.getInstance().setBotFreeze(auctionId, true);

                    User prevLeader = currentAuction.getHighestBidder();
                    double prevPrice = currentAuction.getCurrentPrice();

                    // Rút con Bot này ra khỏi đầu hàng và ném trả về CUỐI HÀNG LUÔN
                    queue.poll();
                    queue.add(currentBot);

                    // Cập nhật trạng thái sàn đấu giá
                    User botUser = new User();
                    botUser.setEmail(currentBot.getEmail());
                    String safeBotName = (botUserInfo.getUsername() != null) ? botUserInfo.getUsername() : currentBot.getEmail();
                    botUser.setUsername(safeBotName);

                    currentAuction.setCurrentPrice(nextBotPrice);
                    currentAuction.setHighestBidder(botUser);
                    currentAuction.setLeaderName(safeBotName);

                    // Ghi lịch sử giao dịch
                    BidTransaction botTransaction = new BidTransaction();
                    botTransaction.setBidder(botUser);
                    botTransaction.setBidAmount(nextBotPrice);
                    if (currentAuction.getBiddingHistory() == null) {
                        currentAuction.setBiddingHistory(new ArrayList<>());
                    }
                    currentAuction.getBiddingHistory().add(botTransaction);

                    context.updateAuction(currentAuction);

                    // Hoàn tiền cho người bị vượt giá
                    if (prevLeader != null) {
                        UserDao.getInstance().depositMoney(prevLeader.getEmail(), prevPrice);
                        updateClientBalance(context, gson, prevLeader.getEmail());
                    }

                    // Bắn thông báo Realtime
                    broadcastNewBid(context, gson, productId, nextBotPrice, safeBotName);
                    updateClientBalance(context, gson, currentBot.getEmail());

                    LOGGER.info("[BOT BID THÀNH CÔNG] Bot " + currentBot.getEmail() + " ăn đỉnh: " + nextBotPrice);
                    LOGGER.info("[KHÓA TUYỆT ĐỐI] Toàn bộ hàng đợi Bot của phiên này chính thức ĐÓNG BĂNG trong 10 giây.");

                    // Hẹn giờ mở khóa sau 10 giây
                    botScheduler.schedule(() -> {
                        synchronized (currentAuction) {
                            if ("ACTIVE".equals(currentAuction.getStatus())) {
                                // TẮT CỜ ĐÓNG BĂNG
                                AuctionManager.getInstance().setBotFreeze(auctionId, false);
                                LOGGER.info("[MỞ KHÓA] Hết 10 giây đóng băng, giải phóng hàng đợi Bot để tiếp tục chiến đấu!");

                                // Gọi lại bộ kích hoạt để lượt quét mới diễn ra bình thường
                                triggerBotWar(context, gson, productId, currentAuction);
                            } else {
                                // Phòng hờ nếu phiên kết thúc trong 10s này thì dọn kho luôn
                                AuctionManager.getInstance().setBotFreeze(auctionId, false);
                            }
                        }
                    }, 10, TimeUnit.SECONDS);

                    // Thoát hoàn toàn khỏi vòng lặp và luồng xử lý
                    return;
                }
            }
        });
    }

    public static void broadcastNewBid(ServerContext context, Gson gson, String productId, double newPrice, String leaderName) {
        Response broadcastRes = new Response(MessageType.BROADCAST_NEW_BID, "SUCCESS", "Có mức giá mới!");
        broadcastRes.getData().put("newPrice", newPrice);
        broadcastRes.getData().put("leaderName", leaderName);
        broadcastRes.getData().put("productId", productId);

        String message = gson.toJson(broadcastRes);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) client.send(message);
        }
    }

    public static void updateClientBalance(ServerContext context, Gson gson, String email) {
        try {
            User u = UserDao.getInstance().getUserByEmail(email);
            if (u == null) return;
            Response res = new Response("GET_BALANCE_RESPONSE", "SUCCESS", "Cập nhật số dư");
            res.getData().put("balance", u.getBalance());
            String msg = gson.toJson(res);
            for (WebSocket client : context.getConnectedClients()) {
                if (email.equals(context.getUserByConn(client)) && client.isOpen()) {
                    client.send(msg);
                    break;
                }
            }
        } catch (Exception e) { /** log */ }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.PLACE_BID_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}