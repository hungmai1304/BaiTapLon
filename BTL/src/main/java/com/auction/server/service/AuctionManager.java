package com.auction.server.service;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.auction.AutoBidConfig;
import com.auction.common.model.product.Product;
import com.auction.common.model.user.User;
import com.auction.server.dao.ProductDao;
import com.auction.server.dao.AuctionDao;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.bidding.PlaceBidHandler;
import com.auction.server.model.ServerContext;
import com.auction.common.utils.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuctionManager {
    private static final Logger LOGGER = Logger.getLogger(AuctionManager.class.getName());

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final Map<String, Queue<AutoBidConfig>> botQueuesMap = new ConcurrentHashMap<>();
    private final Map<String, Boolean> botFreezeMap = new ConcurrentHashMap<>();

    // --- SỬA SINGLETON: Chống lỗi NoClassDefFoundError bằng Holder Class ---
    private static class Holder {
        private static final AuctionManager INSTANCE = new AuctionManager();
    }

    public static AuctionManager getInstance() {
        return Holder.INSTANCE;
    }

    private AuctionManager() {
        // Khởi chạy bộ quét động cơ an toàn
        startAutoAuctionEngine();
    }

    public boolean isBotFrozen(String auctionId) {
        return botFreezeMap.getOrDefault(auctionId, false);
    }

    public void setBotFreeze(String auctionId, boolean freeze) {
        if (freeze) {
            botFreezeMap.put(auctionId, true);
        } else {
            botFreezeMap.remove(auctionId);
        }
    }

    public Queue<AutoBidConfig> getBotQueue(String auctionId) {
        if (auctionId == null) return null;
        return botQueuesMap.computeIfAbsent(auctionId, k -> new ConcurrentLinkedQueue<>());
    }

    private void startAutoAuctionEngine() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndStartIncomingAuctions();
            } catch (Exception e) {
                // SỬA: Log đầy đủ cả nguyên nhân lỗi (Stack Trace) thay vì nuốt chữ
                LOGGER.log(Level.SEVERE, "[Engine] Lỗi nghiêm trọng khi quét mở phiên mới", e);
            }
        }, 1, 2, TimeUnit.SECONDS);
    }

    private void checkAndStartIncomingAuctions() {
        ServerContext context = ServerContext.getInstance();
        if (context == null || context.getActiveAuctions() == null) return;

        LocalDateTime now = LocalDateTime.now();
        List<Auction> incomingAuctions = new ArrayList<>();

        for (Auction a : context.getActiveAuctions()) {
            if (a != null && "PENDING".equals(a.getStatus()) && a.getStartTime() != null && !now.isBefore(a.getStartTime())) {
                incomingAuctions.add(a);
            }
        }

        for (Auction auction : incomingAuctions) {
            boolean shouldTriggerBot = false;
            long delayMillis = 0;

            // TÁCH KHÓA: Chỉ đồng bộ RAM cực nhanh
            synchronized (auction) {
                if (!"PENDING".equals(auction.getStatus())) continue;

                auction.setStatus("ACTIVE");
                if (auction.getProduct() != null) {
                    auction.getProduct().setStatus(ProductStatus.ON_AUCTION);
                }

                context.updateAuction(auction);
                LOGGER.info("[KÍCH HOẠT] Phiên Đấu Giá ID: " + auction.getId() + " ĐÃ LÊN SÀN!");

                if (auction.getEndTime() != null) {
                    delayMillis = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();
                }
                shouldTriggerBot = (auction.getProduct() != null);
            }

            // Gửi dữ liệu mạng bên ngoài khối Synchronized để giải phóng luồng
            broadcastNewAuction(auction);

            // Đặt lịch đóng phiên bất đồng bộ
            if (delayMillis > 0) {
                final Auction finalAuction = auction;
                scheduler.schedule(() -> {
                    LOGGER.info("[TIMER REALTIME] Thực hiện ĐÓNG PHIÊN ID: " + finalAuction.getId());
                    endAuction(finalAuction);
                }, delayMillis, TimeUnit.MILLISECONDS);
            } else if (auction.getEndTime() != null) {
                endAuction(auction);
            }

            // Kích hoạt cuộc chiến Bot tự động
            if (shouldTriggerBot) {
                PlaceBidHandler.triggerBotWar(context, gson, auction.getProduct().getId(), auction);
            }
        }
    }

    private void broadcastNewAuction(Auction auction) {
        try {
            ServerContext context = ServerContext.getInstance();
            if (context.getServer() == null) return;

            Response response = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Có phiên đấu giá mới vừa lên sàn!");
            response.getData().put("auction", auction);
            String message = gson.toJson(response);

            for (WebSocket conn : context.getServer().getConnections()) {
                if (conn != null && conn.isOpen()) {
                    conn.send(message);
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Lỗi phát tin phiên mới: " + e.getMessage());
        }
    }

    public void endAuction(Auction auction) {
        if (auction == null) return;
        ServerContext context = ServerContext.getInstance();

        String winnerEmail = null;
        String winnerName = "Không có";
        double finalPrice = 0;
        String thongBaoChung = "";
        Product product = null;
        String sellerEmail = null;

        // TÁCH BIẾN RA NGOÀI ĐỂ XỬ LÝ KHÔNG GIỮ LOCK LÂU
        synchronized (auction) {
            if ("COMPLETED".equals(auction.getStatus())) return;
            auction.setStatus("COMPLETED");

            product = auction.getProduct();
            finalPrice = auction.getCurrentPrice();

            if (product != null) {
                if (auction.getHighestBidder() != null) {
                    winnerEmail = auction.getHighestBidder().getEmail();
                    winnerName = auction.getHighestBidder().getUsername() != null ? auction.getHighestBidder().getUsername() : winnerEmail;
                    auction.setLeaderName(winnerName);
                    product.setStatus(ProductStatus.SOLD);
                    product.setCurrentPrice(finalPrice);
                    thongBaoChung = "Chúc mừng " + winnerName + " đã chốt đơn sản phẩm '" + product.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";

                    if (product.getOwner() != null) {
                        if (product.getOwner().getEmail() != null && !product.getOwner().getEmail().trim().isEmpty()) {
                            sellerEmail = product.getOwner().getEmail();
                        } else if (product.getOwner().getId() != null) {
                            User sellerFromDB = UserDao.getInstance().findById(product.getOwner().getId());
                            if (sellerFromDB != null) sellerEmail = sellerFromDB.getEmail();
                        }
                    }
                } else {
                    product.setStatus(ProductStatus.AVAILABLE);
                    finalPrice = product.getStartPrice();
                    thongBaoChung = "Rất tiếc, sản phẩm '" + product.getName() + "' đã hết giờ mà không có ai đặt giá!";

                    if (product.getOwner() != null) {
                        sellerEmail = product.getOwner().getEmail();
                    }
                }

                if (product.getStatus() != ProductStatus.SOLD) {
                    product.setStartTime(null);
                    product.setEndTime(null);
                }
            }
        } // <<< THOÁT KHÓA NGAY TẠI ĐÂY - TOÀN BỘ PHẦN DƯỚI CHỈ LÀ GHI DB VÀ GỬI MẠNG

        if (product != null) {
            try {
                // 1. Xử lý cộng tiền cho người bán trong DB độc lập
                if (product.getStatus() == ProductStatus.SOLD && sellerEmail != null) {
                    UserDao.getInstance().depositMoney(sellerEmail, finalPrice);
                    PlaceBidHandler.updateClientBalance(context, gson, sellerEmail);

                    String thongBaoRieng = "Sản phẩm '" + product.getName() + "' của bạn đã bán thành công. Bạn nhận được: " + String.format("%,.0fđ", finalPrice);
                    sendPrivateNotification(context, sellerEmail, thongBaoRieng);
                } else if (sellerEmail != null) {
                    String thongBaoRieng = "Sản phẩm '" + product.getName() + "' của bạn đã kết thúc mà không có ai đặt giá.";
                    sendPrivateNotification(context, sellerEmail, thongBaoRieng);
                }

                // 2. Cập nhật trạng thái sản phẩm vào DB
                ProductDao.getInstance().editProduct(product);

                // 3. Lưu lịch sử đấu giá
                AuctionDao.getInstance().saveCompletedAuction(String.valueOf(auction.getId()), product.getId(), winnerEmail, finalPrice);

                // 4. Phát thông báo kết quả ra toàn sàn
                Response publicRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoChung);
                publicRes.getData().put("auction", auction);
                publicRes.getData().put("winnerName", winnerName);
                String publicMsg = gson.toJson(publicRes);

                for (WebSocket client : context.getServer().getConnections()) {
                    if (client != null && client.isOpen()) client.send(publicMsg);
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Lỗi khi lưu/bắn tin kết thúc đấu giá", e);
            }
        }

        // Dọn dẹp RAM sạch sẽ sau khi hoàn tất phiên
        context.removeAuction(auction.getId());
        botQueuesMap.remove(auction.getId());
    }

    private void sendPrivateNotification(ServerContext context, String email, String messageStr) {
        if (email == null) return;
        Response res = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", messageStr);
        String msg = gson.toJson(res);
        for (WebSocket client : context.getServer().getConnections()) {
            if (client != null && client.isOpen() && email.equals(context.getUserByConn(client))) {
                client.send(msg);
                break;
            }
        }
    }
}