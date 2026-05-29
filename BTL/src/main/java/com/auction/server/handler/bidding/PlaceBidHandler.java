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

@CommandMap(value = MessageType.PLACE_BID_REQUEST)
public class PlaceBidHandler implements IMessageHandler {

    // Hệ thống ThreadPool quản lý bất đồng bộ các luồng trả tiền cũ và xử lý AI Bot của File 2
    private static final ScheduledExecutorService botScheduler = Executors.newScheduledThreadPool(4);

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

            // KIỂM TRA & CẬP NHẬT TRẠNG THÁI (Thread-safe trên RAM bằng synchronized)
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

                // CHIẾN THUẬT HOLD/REFUND: [HOLD] Thử trừ tiền người đặt giá mới trong DB
                boolean isHoldSuccess = UserDao.getInstance().withdrawMoney(userEmail, bidAmount);
                if (!isHoldSuccess) {
                    sendError(conn, gson, "Số dư ví không đủ để thực hiện lượt đặt giá " + String.format("%,.0fđ", bidAmount) + "!");
                    return;
                }

                // Ghi nhận thông tin người cũ để đẩy xuống tiến trình hoàn tiền (Refund) bất đồng bộ
                previousLeader = currentAuction.getHighestBidder();
                previousBid = currentAuction.getCurrentPrice();

                // Cập nhật dữ liệu người dẫn đầu mới vào RAM
                User newLeader = new User();
                newLeader.setEmail(userEmail);
                newLeader.setUsername(safeUserName);

                currentAuction.setCurrentPrice(bidAmount);
                currentAuction.setHighestBidder(newLeader);
                currentAuction.setLeaderName(safeUserName);

                // Ghi sổ lịch sử giao dịch bằng BidTransaction (Tương thích giữ nguyên cấu trúc gốc)
                BidTransaction transaction = new BidTransaction();
                transaction.setId(String.valueOf(currentAuction.getId()));
                transaction.setBidder(newLeader);
                transaction.setBidAmount(bidAmount);
                transaction.setTimeCreated(LocalDateTime.now());

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

                // Đồng bộ cập nhật số dư hiển thị cho người vừa đặt giá xong (File 2)
                updateClientBalance(context, gson, userEmail);

                // PHÁT LOA CHO CẢ SÀN: Đồng bộ đính kèm tham số newEndTime phục vụ Anti-Sniping của File 1
                broadcastNewBidWithExtension(context, gson, productId, bidAmount, safeUserName, isExtended ? currentAuction.getEndTime() : null);
            }

            // [REFUND] Tiến hành trả lại tiền cho người bị ghi đè giá bằng Async Thread tránh nghẽn luồng chính (File 2)
            if (previousLeader != null) {
                final User finalPrevLeader = previousLeader;
                final double finalPrevBid = previousBid;
                botScheduler.submit(() -> {
                    UserDao.getInstance().depositMoney(finalPrevLeader.getEmail(), finalPrevBid);
                    updateClientBalance(context, gson, finalPrevLeader.getEmail());
                    System.out.println("   [Refund] Đã hoàn trả " + String.format("%,.0fđ", finalPrevBid) + " cho người cũ: " + finalPrevLeader.getEmail());
                });
            }

            // KÍCH HOẠT HỆ THỐNG AI BOT PHẢN CÔNG (Giữ nguyên 100% logic File 2)
            triggerBotWar(context, gson, productId, currentAuction);

            // Phản hồi riêng cho Client đặt giá thành công
            Response successRes = new Response(MessageType.PLACE_BID_RESPONSE, "SUCCESS", "Chúc mừng! Bạn đang là người dẫn đầu!");
            conn.send(gson.toJson(successRes));

            System.out.println("-> [Bid] " + userEmail + " vừa ra giá " + bidAmount + " cho SP: " + productId);

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

        // Kiểm tra đóng băng 10 giây chống spam luồng Bot
        if (AuctionManager.getInstance().isBotFrozen(auctionId)) {
            return;
        }

        Queue<AutoBidConfig> queue = AuctionManager.getInstance().getBotQueue(auctionId);
        if (queue == null || queue.isEmpty()) return;

        botScheduler.submit(() -> {
            synchronized (currentAuction) {
                // Kiểm tra lại trong khối đồng bộ tránh Race Condition
                if (!"ACTIVE".equals(currentAuction.getStatus()) || AuctionManager.getInstance().isBotFrozen(auctionId)) {
                    return;
                }

                while (!queue.isEmpty()) {
                    AutoBidConfig currentBot = queue.peek();
                    if (currentBot == null) return;

                    // TH 1: Bot đang dẫn đầu -> Đẩy xuống cuối hàng và chuyển con tiếp theo
                    if (currentAuction.getHighestBidder() != null
                            && currentBot.getEmail().equals(currentAuction.getHighestBidder().getEmail())) {
                        queue.poll();
                        queue.add(currentBot);
                        continue;
                    }

                    double nextBotPrice = (currentAuction.getHighestBidder() == null)
                            ? currentAuction.getStartPrice()
                            : (currentAuction.getCurrentPrice() + currentBot.getStepPrice());

                    // TH 2: Vượt quá giá trần cài đặt của cấu hình cấu hình Bot -> Loại khỏi hàng đợi
                    if (nextBotPrice > currentBot.getMaxPrice()) {
                        queue.poll();
                        System.out.println("[BOT OUT] Bot " + currentBot.getEmail() + " vượt trần, bị loại.");
                        continue;
                    }

                    // TH 3: Ví tiền của Bot trong DB không đủ đáp ứng mức giá mới -> Loại khỏi hàng đợi
                    User botUserInfo = UserDao.getInstance().getUserByEmail(currentBot.getEmail());
                    if (botUserInfo == null || botUserInfo.getBalance() < nextBotPrice) {
                        queue.poll();
                        System.out.println("[BOT OUT] Bot " + currentBot.getEmail() + " hết tiền hoặc không tồn tại, bị loại.");
                        continue;
                    }

                    // Thực hiện rút tiền tạm giữ tài khoản Bot thành công
                    boolean botHold = UserDao.getInstance().withdrawMoney(currentBot.getEmail(), nextBotPrice);
                    if (!botHold) {
                        queue.poll();
                        continue;
                    }

                    // KHÓA ĐÓNG BĂNG HÀNG ĐỢI NGAY LẬP TỨC để ngăn chặn spam
                    AuctionManager.getInstance().setBotFreeze(auctionId, true);

                    User prevLeader = currentAuction.getHighestBidder();
                    double prevPrice = currentAuction.getCurrentPrice();

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

                    // Hoàn tiền cho người bị Bot vượt giá
                    if (prevLeader != null) {
                        UserDao.getInstance().depositMoney(prevLeader.getEmail(), prevPrice);
                        updateClientBalance(context, gson, prevLeader.getEmail());
                    }

                    // Bắn thông báo Realtime toàn sàn (Tích hợp tham số newEndTime mở rộng từ File 1)
                    broadcastNewBidWithExtension(context, gson, productId, nextBotPrice, safeBotName, isBotExtended ? currentAuction.getEndTime() : null);
                    updateClientBalance(context, gson, currentBot.getEmail());

                    System.out.println("[BOT BID THÀNH CÔNG] Bot " + currentBot.getEmail() + " ăn đỉnh: " + nextBotPrice);
                    System.out.println("[KHÓA TUYỆT ĐỐI] Toàn bộ hàng đợi Bot của phiên này chính thức ĐÓNG BĂNG trong 10 giây.");

                    // Bộ lập lịch Hẹn giờ mở khóa Bot sau 10 giây đóng băng từ File 2
                    botScheduler.schedule(() -> {
                        synchronized (currentAuction) {
                            if ("ACTIVE".equals(currentAuction.getStatus())) {
                                AuctionManager.getInstance().setBotFreeze(auctionId, false);
                                System.out.println("[MỞ KHÓA] Hết 10 giây đóng băng, giải phóng hàng đợi Bot để tiếp tục chiến đấu!");

                                // Quét đợt kích hoạt chiến đấu mới
                                triggerBotWar(context, gson, productId, currentAuction);
                            } else {
                                AuctionManager.getInstance().setBotFreeze(auctionId, false);
                            }
                        }
                    }, 10, TimeUnit.SECONDS);

                    return;
                }
            }
        });
    }

    // HÀM PHÁT LOA TOÀN SÀN NHẢY SỐ REALTIME - HỖ TRỢ ĐÍNH KÈM GIA HẠN ANTI-SNIPING (Từ File 1)
    private static void broadcastNewBidWithExtension(ServerContext context, Gson gson, String productId, double newPrice, String leaderName, LocalDateTime newEndTime) {
        Response broadcastRes = new Response(MessageType.BROADCAST_NEW_BID, "SUCCESS", "Có mức giá mới!");
        broadcastRes.getData().put("newPrice", newPrice);
        broadcastRes.getData().put("leaderName", leaderName);
        broadcastRes.getData().put("productId", productId);

        if (newEndTime != null) {
            broadcastRes.getData().put("newEndTime", newEndTime.toString());
        }

        String message = gson.toJson(broadcastRes);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
        System.out.println("   -> [Broadcast Bid] Đã thông báo giá mới (" + newPrice + ") cho toàn Server." + (newEndTime != null ? " [ĐÍNH KÈM GIA HẠN]" : ""));
    }

    // ĐỒNG BỘ SỐ DƯ VÍ TÀI KHOẢN NGƯỜI DÙNG REALTIME (Từ File 2)
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
        } catch (Exception e) {
            System.err.println("[Balance Sync Error] Không thể đồng bộ số dư: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.PLACE_BID_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}