package com.auction.server.handler.bidding;

import com.auction.common.model.auction.AutoBidConfig;
import com.auction.protocol.MessageType;
import com.auction.common.model.auction.Auction;
import com.auction.common.model.auction.BidTransaction;
import com.auction.common.model.product.Product; // Import thêm class Product
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

            // BẢO MẬT: Lấy từ session socket
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Bạn chưa đăng nhập hoặc phiên làm việc hết hạn!");
                return;
            }

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Lỗi: Thiếu ID sản phẩm!");
                return;
            }

            // Kiểm tra thông tin User và trạng thái Blacklist
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

            // =========================================================================
            // NGHIỆP VỤ MỚI: CHẶN NGƯỜI BÁN TỰ ĐẶT GIÁ SẢN PHẨM CỦA CHÍNH MÌNH
            // =========================================================================
            if (currentAuction.getProduct() != null) {
                Product product = (Product) currentAuction.getProduct();
                if (product.getOwner() != null) {
                    String sellerEmail = product.getOwner().getEmail();
                    String sellerId = product.getOwner().getId();

                    // So sánh email hoặc ID của tài khoản đang kết nối với chủ sản phẩm
                    if (userEmail.equalsIgnoreCase(sellerEmail) || currentUser.getId().equals(sellerId)) {
                        System.err.println("[PlaceBidHandler] Từ chối Seller tự đấu giá: " + userEmail);
                        sendError(conn, gson, "Lỗi quy định: Bạn là người bán sản phẩm này, không được phép tự đặt giá!");
                        return;
                    }
                }
            }
            // =========================================================================

            if ("PENDING".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Chưa đến giờ! Sản phẩm đang trong thời gian chờ mở sàn.");
                return;
            } else if ("COMPLETED".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Muộn rồi! Phiên đấu giá này đã kết thúc.");
                return;
            }

            User previousLeader = null;
            double previousBid = 0;

            // ĐỒNG BỘ HÓA LUỒNG TRÊN RAM (NHANH CHÓNG): Chống Race Condition
            synchronized (currentAuction) {
                double minRequiredPrice = (currentAuction.getHighestBidder() == null)
                        ? currentAuction.getStartPrice()
                        : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

                if (bidAmount < minRequiredPrice) {
                    sendError(conn, gson, "Giá đưa ra đã bị người khác dẫn trước! Vui lòng đặt tối thiểu: " + String.format("%,.0f", minRequiredPrice));
                    return;
                }

                // Thực hiện trừ tiền người mới qua DB IO
                boolean isHoldSuccess = UserDao.getInstance().withdrawMoney(userEmail, bidAmount);
                if (!isHoldSuccess) {
                    sendError(conn, gson, "Số dư ví không đủ để thực hiện lượt đặt giá " + String.format("%,.0fđ", bidAmount) + "!");
                    return;
                }

                previousLeader = currentAuction.getHighestBidder();
                previousBid = currentAuction.getCurrentPrice();

                // Tiến hành ghi nhận người dẫn đầu mới lên RAM
                User newLeader = new User();
                newLeader.setEmail(userEmail);
                newLeader.setUsername(currentUser.getUsername());

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

            // HOÀN TIỀN CHO NGƯỜI CŨ (Xử lý ngoài khối synchronized để tối ưu hiệu năng)
            if (previousLeader != null) {
                final User finalPreviousLeader = previousLeader;
                final double finalPreviousBid = previousBid;
                botExecutor.submit(() -> {
                    UserDao.getInstance().depositMoney(finalPreviousLeader.getEmail(), finalPreviousBid);
                    System.out.println("[Refund Async] Đã trả lại " + String.format("%,.0fđ", finalPreviousBid) + " cho: " + finalPreviousLeader.getEmail());
                });
            }

            // Phát loa Real-time cho toàn sàn nhận giá mới
            broadcastNewBid(context, gson, productId, bidAmount, userEmail);

            // Kích hoạt cuộc chiến Bot
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

    public static void triggerBotWar(ServerContext context, Gson gson, String productId, Auction currentAuction) {
        if (currentAuction.getRegisteredBots() == null || currentAuction.getRegisteredBots().isEmpty()) {
            return;
        }

        botExecutor.submit(() -> {
            boolean keepFighting = true;
            int safetyCounter = 0;

            while (keepFighting && safetyCounter < 100) {
                keepFighting = false;
                safetyCounter++;

                synchronized (currentAuction) {
                    if (!"ACTIVE".equals(currentAuction.getStatus())) {
                        break;
                    }

                    for (AutoBidConfig bot : currentAuction.getRegisteredBots()) {
                        if (currentAuction.getHighestBidder() != null && bot.getEmail().equals(currentAuction.getHighestBidder().getEmail())) {
                            continue;
                        }

                        double nextBotPrice = (currentAuction.getHighestBidder() == null)
                                ? currentAuction.getStartPrice()
                                : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

                        User botUserInfo = UserDao.getInstance().getUserByEmail(bot.getEmail());
                        if (botUserInfo == null || botUserInfo.getBalance() < nextBotPrice) {
                            continue;
                        }

                        if (nextBotPrice <= bot.getMaxPrice()) {
                            boolean botHold = UserDao.getInstance().withdrawMoney(bot.getEmail(), nextBotPrice);
                            if(!botHold) continue;

                            User prevLeaderBeforeBot = currentAuction.getHighestBidder();
                            double prevPriceBeforeBot = currentAuction.getCurrentPrice();

                            if (prevLeaderBeforeBot != null) {
                                UserDao.getInstance().depositMoney(prevLeaderBeforeBot.getEmail(), prevPriceBeforeBot);
                            }

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
                            break;
                        }
                    }
                }

                if (keepFighting) {
                    try {
                        Thread.sleep(500);
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