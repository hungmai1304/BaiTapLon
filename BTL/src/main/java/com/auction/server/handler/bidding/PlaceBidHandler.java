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

    // Luồng pool cố định chuyên gánh các tác vụ delay, xử lý Bot và I/O mạng ngầm
    private static final ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors())
    );

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("productId");

            // Xử lý an toàn: Lấy bidAmount kèm kiểm tra null chống crash từ File 1
            Object bidAmountObj = data.get("bidAmount");
            if (bidAmountObj == null) {
                sendError(conn, gson, "Lỗi: Không tìm thấy mức giá đặt (bidAmount)!");
                return;
            }
            double bidAmount = ((Number) bidAmountObj).doubleValue();

            // BẢO MẬT CAO: Lấy email từ session kết nối (File 1) thay vì tin tưởng Client hoàn toàn
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Bạn chưa đăng nhập hoặc phiên làm việc không hợp lệ!");
                return;
            }

            if (productId == null) {
                sendError(conn, gson, "Lỗi: Thiếu ID sản phẩm (productId)!");
                return;
            }

            // Kiểm tra trạng thái tài khoản người dùng từ File 2
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null || "BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                sendError(conn, gson, "Tài khoản không hợp lệ hoặc đã bị khóa khỏi sàn!");
                return;
            }

            // Tìm Phiên Đấu Giá chứa Sản phẩm này trên RAM
            Auction currentAuction = context.getAuctionByProductId(productId);
            if (currentAuction == null) {
                sendError(conn, gson, "Thất bại: Sản phẩm này hiện không nằm trong phiên đấu giá nào!");
                return;
            }

            User previousLeader = null;
            double previousBid = 0;
            String safeUserName = (currentUser.getUsername() != null && !currentUser.getUsername().trim().isEmpty())
                    ? currentUser.getUsername() : userEmail;

            boolean userBidSuccessful = false;

            // --- PHẠM VI KHÓA ĐƯỢC THU HẸP TỐI ĐA ---
            synchronized (currentAuction) {
                // Kiểm tra trạng thái nghiêm ngặt từ File 1 & File 2
                if ("PENDING".equals(currentAuction.getStatus())) {
                    sendError(conn, gson, "Chưa đến giờ! Sản phẩm đang trong 30 phút quảng cáo.");
                    return;
                } else if ("COMPLETED".equals(currentAuction.getStatus()) || !"ACTIVE".equals(currentAuction.getStatus())) {
                    sendError(conn, gson, "Muộn rồi! Phiên đấu giá này không ở trạng thái sẵn sàng hoặc đã kết thúc.");
                    return;
                }

                // Không cho phép người đang dẫn đầu tự đè giá chính mình (File 2)
                if (currentAuction.getHighestBidder() != null
                        && currentAuction.getHighestBidder().getEmail().equalsIgnoreCase(userEmail)) {
                    sendError(conn, gson, "Bạn đang dẫn đầu, không cần đặt thêm giá!");
                    return;
                }

                // KIỂM TRA LUẬT ĐẤU GIÁ (Giá mới phải >= Giá hiện tại + Bước giá)
                double minRequiredPrice = (currentAuction.getHighestBidder() == null)
                        ? currentAuction.getStartPrice()
                        : (currentAuction.getCurrentPrice() + currentAuction.getStepPrice());

                if (bidAmount < minRequiredPrice) {
                    sendError(conn, gson, "Giá bạn đưa ra quá thấp! Giá tối thiểu cần đặt: " + String.format("%,.0fđ", minRequiredPrice));
                    return;
                }

                // Chấp nhận trừ tiền DB (Lệnh này buộc phải đồng bộ để tránh chi tiêu vượt mức)
                boolean isHoldSuccess = UserDao.getInstance().withdrawMoney(userEmail, bidAmount);
                if (!isHoldSuccess) {
                    sendError(conn, gson, "Số dư ví không đủ để thực hiện lượt đặt giá " + String.format("%,.0fđ", bidAmount) + "!");
                    return;
                }

                // Sao chép nhanh dữ liệu người dẫn đầu cũ để xử lý hoàn tiền SAU khi mở khóa
                previousLeader = currentAuction.getHighestBidder();
                previousBid = currentAuction.getCurrentPrice();

                // Cập nhật dữ liệu người dẫn đầu mới vào RAM
                User newLeader = new User();
                newLeader.setEmail(userEmail);
                newLeader.setUsername(safeUserName);

                currentAuction.setCurrentPrice(bidAmount);
                currentAuction.setHighestBidder(newLeader);
                currentAuction.setLeaderName(safeUserName);

                BidTransaction transaction = new BidTransaction();
                transaction.setId(String.valueOf(currentAuction.getId()));
                transaction.setBidder(newLeader);
                transaction.setBidAmount(bidAmount);

                if (currentAuction.getBiddingHistory() == null) {
                    currentAuction.setBiddingHistory(new ArrayList<>());
                }
                currentAuction.getBiddingHistory().add(transaction);

                // =========================================================================
                // TÍNH NĂNG ANTI-SNIPING (Từ File 1): Tự động gia hạn nếu Bid ở 30 giây cuối
                // =========================================================================
                LocalDateTime now = LocalDateTime.now();
                long secondsLeft = java.time.Duration.between(now, currentAuction.getEndTime()).getSeconds();
                boolean isExtended = false;

                if (secondsLeft <= 30 && secondsLeft > 0) {
                    // Gia hạn thêm 30 giây vào cấu trúc thời gian kết thúc của phiên
                    LocalDateTime newEndTime = currentAuction.getEndTime().plusSeconds(30);
                    currentAuction.setEndTime(newEndTime);
                    isExtended = true;
                    System.out.println("🔥 [Anti-Sniping] Phiên " + currentAuction.getId() + " được gia hạn thêm 30s. Kết thúc mới: " + newEndTime);
                }

                // Lưu trạng thái cập nhật mới nhất vào Server Context
                context.updateAuction(currentAuction);
                userBidSuccessful = true;
            }
            // --- KẾT THÚC KHÓA (SÀN ĐẤU GIÁ ĐÃ ĐƯỢC GIẢI PHÓNG CHO NGƯỜI KHÁC BẤM) ---

            // Thực hiện các tác vụ biên và thông báo I/O ở môi trường tự do bên ngoài lock
            if (userBidSuccessful) {
                if (previousLeader != null) {
                    final User finalPrevLeader = previousLeader;
                    final double finalPrevBid = previousBid;
                    botScheduler.submit(() -> {
                        UserDao.getInstance().depositMoney(finalPrevLeader.getEmail(), finalPrevBid);
                        updateClientBalance(context, gson, finalPrevLeader.getEmail());
                    });
                }

                // Phát loa và cập nhật số dư O(1) cực nhanh không lo nghẽn luồng nghiệp vụ
                broadcastNewBid(context, gson, productId, bidAmount, safeUserName);
                updateClientBalance(context, gson, userEmail);

                // Kích hoạt trận chiến Bot phản công độc lập
                triggerBotWar(context, gson, productId, currentAuction);

                Response successRes = new Response(MessageType.PLACE_BID_RESPONSE, "SUCCESS", "Bạn đang dẫn đầu!");
                conn.send(gson.toJson(successRes));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi Server khi xử lý đấu giá: " + e.getMessage());
        }
    }

    // =========================================================================
    // HỆ THỐNG QUẢN LÝ ROBOT TỰ ĐỘNG (BOT WAR) - GIỮ NGUYÊN 100% TỪ FILE 2
    // =========================================================================
    public static void triggerBotWar(ServerContext context, Gson gson, String productId, Auction currentAuction) {
        String auctionId = currentAuction.getId();

        // 1. KIỂM TRA NGAY TỪ ĐẦU: Nếu hàng đợi đang bị đóng băng thì CHẶN LUÔN
        if (AuctionManager.getInstance().isBotFrozen(auctionId)) {
            return;
        }

        Queue<AutoBidConfig> queue = AuctionManager.getInstance().getBotQueue(auctionId);
        if (queue == null || queue.isEmpty()) return;

        botScheduler.submit(() -> {
            User prevLeaderToRefund = null;
            double prevPriceToRefund = 0;
            boolean botBidSuccessful = false;
            String successBotEmail = null;
            double successBotPrice = 0;
            String successBotName = null;

            // --- KHÓA CHỈ DÙNG ĐỂ KIỂM TRA TRẠNG THÁI VÀ ĐỔI BIẾN TRÊN RAM ---
            synchronized (currentAuction) {
                if (!"ACTIVE".equals(currentAuction.getStatus()) || AuctionManager.getInstance().isBotFrozen(auctionId)) {
                    return;
                }

                // MÀNG LỌC CHỐNG VÒNG LẶP VÔ HẠN (Infinite Loop Protection)
                int keysRolled = 0;
                int totalBots = queue.size();

                while (!queue.isEmpty() && keysRolled < totalBots) {
                    AutoBidConfig currentBot = queue.peek();
                    if (currentBot == null) return;

                    // TH 1: Bot đầu hàng đang dẫn đầu -> đẩy xuống cuối hàng, tăng biến đếm để tránh lặp vô hạn
                    if (currentAuction.getHighestBidder() != null
                            && currentBot.getEmail().equals(currentAuction.getHighestBidder().getEmail())) {
                        queue.poll();
                        queue.add(currentBot);
                        keysRolled++; // Đánh dấu đã duyệt qua 1 con bot vô dụng ở lượt này
                        continue;
                    }

                    double nextBotPrice = (currentAuction.getHighestBidder() == null)
                            ? currentAuction.getStartPrice()
                            : (currentAuction.getCurrentPrice() + currentBot.getStepPrice());

                    // TH 2: Vượt quá giá trần -> Cút lập tức, reset bộ đếm vì cấu trúc hàng đợi đã thay đổi
                    if (nextBotPrice > currentBot.getMaxPrice()) {
                        queue.poll();
                        LOGGER.info("[BOT OUT] Bot " + currentBot.getEmail() + " vượt trần, bị loại.");
                        totalBots = queue.size();
                        keysRolled = 0;
                        continue;
                    }

                    // TH 3: Kiểm tra ví tiền trong DB -> Hết tiền cút lập tức, reset bộ đếm
                    User botUserInfo = UserDao.getInstance().getUserByEmail(currentBot.getEmail());
                    if (botUserInfo == null || botUserInfo.getBalance() < nextBotPrice) {
                        queue.poll();
                        LOGGER.info("[BOT OUT] Bot " + currentBot.getEmail() + " hết tiền, bị loại.");
                        totalBots = queue.size();
                        keysRolled = 0;
                        continue;
                    }

                    // Thực hiện rút tiền tạm giữ tài khoản Bot thành công
                    boolean botHold = UserDao.getInstance().withdrawMoney(currentBot.getEmail(), nextBotPrice);
                    if (!botHold) {
                        queue.poll();
                        totalBots = queue.size();
                        keysRolled = 0;
                        continue;
                    }

                    // KHÓA ĐÓNG BĂNG HÀNG ĐỢI NGAY LẬP TỨC để ngăn chặn spam
                    AuctionManager.getInstance().setBotFreeze(auctionId, true);

                    prevLeaderToRefund = currentAuction.getHighestBidder();
                    prevPriceToRefund = currentAuction.getCurrentPrice();

                    // Rút Bot ra khỏi vị trí đầu tiên và ném về cuối hàng đợi
                    queue.poll();
                    queue.add(currentBot);

                    // Thiết lập thông tin thực thể Bot dẫn đầu phiên mới trên RAM
                    User botUser = new User();
                    botUser.setEmail(currentBot.getEmail());
                    String safeBotName = (botUserInfo.getUsername() != null) ? botUserInfo.getUsername() : currentBot.getEmail();
                    botUser.setUsername(safeBotName);

                    currentAuction.setCurrentPrice(nextBotPrice);
                    currentAuction.setHighestBidder(botUser);
                    currentAuction.setLeaderName(safeBotName);

                    // Ghi lịch sử giao dịch Bot
                    BidTransaction botTransaction = new BidTransaction();
                    botTransaction.setId(String.valueOf(currentAuction.getId()));
                    botTransaction.setBidder(botUser);
                    botTransaction.setBidAmount(nextBotPrice);
                    botTransaction.setTimeCreated(LocalDateTime.now());

                    if (currentAuction.getBiddingHistory() == null) {
                        currentAuction.setBiddingHistory(new ArrayList<>());
                    }
                    currentAuction.getBiddingHistory().add(botTransaction);

                    // =========================================================================
                    // TÍNH NĂNG BỔ SUNG: ANTI-SNIPING CHO CẢ LƯỢT ĐẶT CỦA BOT (Đảm bảo an toàn)
                    // =========================================================================
                    LocalDateTime botNow = LocalDateTime.now();
                    long botSecondsLeft = java.time.Duration.between(botNow, currentAuction.getEndTime()).getSeconds();
                    boolean isBotExtended = false;
                    if (botSecondsLeft <= 30 && botSecondsLeft > 0) {
                        LocalDateTime newEndTime = currentAuction.getEndTime().plusSeconds(30);
                        currentAuction.setEndTime(newEndTime);
                        isBotExtended = true;
                        System.out.println("🔥 [Anti-Sniping từ BOT] Phiên " + currentAuction.getId() + " được gia hạn thêm 30s. Kết thúc mới: " + newEndTime);
                    }

                    context.updateAuction(currentAuction);

                    // Gom snapshot dữ liệu thành công để tí ra ngoài Lock xử lý I/O mạng
                    botBidSuccessful = true;
                    successBotEmail = currentBot.getEmail();
                    successBotPrice = nextBotPrice;
                    successBotName = safeBotName;

                    break; // THOÁT NGAY VÒNG LẶP ĐỂ GIẢI PHÓNG ĐỒNG BỘ KHÓA RAM!
                }
            }
            // --- KẾT THÚC KHÓA (SÀN ĐẤU GIÁ ĐÃ ĐƯỢC GIẢI PHÓNG AN TOÀN) ---

            // Tiến hành xử lý I/O mạng độc lập bên ngoài lock
            if (botBidSuccessful) {
                final User finalPrevLeader = prevLeaderToRefund;
                final double finalPrevPrice = prevPriceToRefund;

                if (finalPrevLeader != null) {
                    UserDao.getInstance().depositMoney(finalPrevLeader.getEmail(), finalPrevPrice);
                    updateClientBalance(context, gson, finalPrevLeader.getEmail());
                }

                // Bắn thông báo Realtime
                broadcastNewBid(context, gson, productId, successBotPrice, successBotName);
                updateClientBalance(context, gson, successBotEmail);

                LOGGER.info("[BOT BID THÀNH CÔNG] Bot " + successBotEmail + " ăn đỉnh: " + successBotPrice);
                LOGGER.info("[KHÓA TUYỆT ĐỐI] Toàn bộ hàng đợi Bot của phiên này chính thức ĐÓNG BĂNG trong 10 giây.");

                // Hẹn giờ mở khóa sau 10 giây
                botScheduler.schedule(() -> {
                    synchronized (currentAuction) {
                        if ("ACTIVE".equals(currentAuction.getStatus())) {
                            AuctionManager.getInstance().setBotFreeze(auctionId, false);
                            LOGGER.info("[MỞ KHÓA] Hết 10 giây đóng băng, giải phóng hàng đợi Bot để tiếp tục chiến đấu!");

                            // Gọi lại bộ kích hoạt để lượt quét mới diễn ra bình thường
                            triggerBotWar(context, gson, productId, currentAuction);
                        } else {
                            AuctionManager.getInstance().setBotFreeze(auctionId, false);
                        }
                    }
                }, 10, TimeUnit.SECONDS);
            }
        });
    }

    // TỐI ƯU BẤT ĐỒNG BỘ: Không cho phép việc lặp gửi mạng làm chậm luồng xử lý chính
    public static void broadcastNewBid(ServerContext context, Gson gson, String productId, double newPrice, String leaderName) {
        Response broadcastRes = new Response(MessageType.BROADCAST_NEW_BID, "SUCCESS", "Có mức giá mới!");
        broadcastRes.getData().put("newPrice", newPrice);
        broadcastRes.getData().put("leaderName", leaderName);
        broadcastRes.getData().put("productId", productId);

        if (newEndTime != null) {
            broadcastRes.getData().put("newEndTime", newEndTime.toString());
        }

        String message = gson.toJson(broadcastRes);

        botScheduler.submit(() -> {
            for (WebSocket client : context.getConnectedClients()) {
                if (client != null && client.isOpen()) {
                    try {
                        client.send(message);
                    } catch (Exception ignored) {}
                }
            }
        });
    }

    // SIÊU TỐI ƯU O(1): Tìm thẳng đến đích connection thông qua Email Map, bẻ gãy hoàn toàn vòng lặp quét mảng cũ!
    public static void updateClientBalance(ServerContext context, Gson gson, String email) {
        try {
            User u = UserDao.getInstance().getUserByEmail(email);
            if (u == null) return;
            Response res = new Response("GET_BALANCE_RESPONSE", "SUCCESS", "Cập nhật số dư");
            res.getData().put("balance", u.getBalance());
            String msg = gson.toJson(res);

            // Tìm kiếm kết nối của user qua email với tốc độ O(1)
            WebSocket client = context.getConnByUser(email);
            if (client != null && client.isOpen()) {
                client.send(msg);
            }
        } catch (Exception e) {
            LOGGER.warning("Lỗi cập nhật số dư cho " + email + ": " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.PLACE_BID_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}