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
import java.util.logging.Logger;

public class AuctionManager {
    private static final Logger LOGGER = Logger.getLogger(AuctionManager.class.getName());

    private static AuctionManager instance;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    // Khởi tạo luồng quét ngầm hệ thống
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Bộ lưu trữ hàng đợi Bot an toàn cho từng phiên đấu giá
    private final Map<String, Queue<AutoBidConfig>> botQueuesMap = new ConcurrentHashMap<>();
    // Quản lý trạng thái đóng băng 10s của từng phiên đấu giá
    private final Map<String, Boolean> botFreezeMap = new ConcurrentHashMap<>();

    // Thêm hàm bổ trợ này để các Handler tiện check trạng thái
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

    private AuctionManager() {
        // KHỞI CHẠY BỘ QUÉT: Bây giờ CHỈ quét để KÍCH HOẠT phiên mới
        startAutoAuctionEngine();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public Queue<AutoBidConfig> getBotQueue(String auctionId) {
        if (auctionId == null) return null;
        return botQueuesMap.computeIfAbsent(auctionId, k -> new ConcurrentLinkedQueue<>());
    }

    /**
     * BỘ MÁY QUÉT TỰ ĐỘNG: Định kỳ quét kích hoạt phiên mới lên sàn.
     */
    private void startAutoAuctionEngine() {
        // Vòng lặp quét kích hoạt phiên mới (Giữ nguyên chu kỳ ngắn để mở phiên trơn tru)
        scheduler.scheduleAtFixedRate(() -> {
            try {
                checkAndStartIncomingAuctions();
            } catch (Exception e) {
                LOGGER.severe("[Engine] Lỗi khi quét mở phiên mới: " + e.getMessage());
            }
        }, 0, 2, TimeUnit.SECONDS);

        // --- ĐÃ XÓA BỎ VÒNG LẶP QUÉT ĐÓNG PHIÊN 5 GIÂY VỚ VẨN ---
    }

    /**
     * 1. HÀM TỰ ĐỘNG MỞ PHIÊN VÀ ĐẶT LỊCH ĐÓNG PHIÊN REAL-TIME
     */
    private void checkAndStartIncomingAuctions() {
        ServerContext context = ServerContext.getInstance();
        LocalDateTime now = LocalDateTime.now();

        // Lọc ra tất cả các phiên PENDING đã đến hoặc quá thời gian bắt đầu
        List<Auction> incomingAuctions = new ArrayList<>();
        for (Auction a : context.getActiveAuctions()) {
            if ("PENDING".equals(a.getStatus()) && a.getStartTime() != null && !now.isBefore(a.getStartTime())) {
                incomingAuctions.add(a);
            }
        }

        // Kích hoạt đồng loạt (Song song)
        for (Auction auction : incomingAuctions) {
            synchronized (auction) {
                if (!"PENDING".equals(auction.getStatus())) continue;

                auction.setStatus("ACTIVE");
                if (auction.getProduct() != null) {
                    auction.getProduct().setStatus(ProductStatus.ON_AUCTION);
                }

                context.updateAuction(auction);
                LOGGER.info("[KÍCH HOẠT SONG SONG] Phiên Đấu Giá ID: " + auction.getId() + " ĐÃ LÊN SÀN!");

                broadcastNewAuction(auction);

                // ==================== ĐOẠN NÂNG CẤP XỬ LÝ REAL-TIME CHUẨN ĐÉT ====================
                if (auction.getEndTime() != null) {
                    long delayMillis = Duration.between(LocalDateTime.now(), auction.getEndTime()).toMillis();

                    if (delayMillis > 0) {
                        // Lên lịch đóng phiên độc lập, nổ súng chính xác đến từng mili-giây!
                        scheduler.schedule(() -> {
                            LOGGER.info("[TIMER CHÍNH XÁC] Đã chạm mili-giây kết thúc! Tiến hành ĐÓNG PHIÊN ID: " + auction.getId());
                            endAuction(auction);
                        }, delayMillis, TimeUnit.MILLISECONDS);
                    } else {
                        // Trường hợp bất khả kháng: Vừa mở ra mà hệ thống tính toán đã lỡ quá giờ thì hạ màn luôn
                        endAuction(auction);
                    }
                }
                // ===============================================================================

                // Kích hoạt Bot War riêng cho phiên này
                if (auction.getProduct() != null) {
                    PlaceBidHandler.triggerBotWar(context, gson, auction.getProduct().getId(), auction);
                }
            }
        }
    }

    private void broadcastNewAuction(Auction auction) {
        ServerContext context = ServerContext.getInstance();
        Response response = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Có phiên đấu giá mới vừa lên sàn!");
        response.getData().put("auction", auction);
        String message = gson.toJson(response);

        for (WebSocket conn : context.getServer().getConnections()) {
            if (conn.isOpen()) conn.send(message);
        }
    }

    /**
     * KẾT THÚC PHIÊN ĐẤU GIÁ ĐỘC LẬP REAL-TIME
     */
    public void endAuction(Auction auction) {
        if (auction == null) return;
        ServerContext context = ServerContext.getInstance();

        String winnerEmail = null;
        String winnerName = "Không có";
        double finalPrice = auction.getCurrentPrice();
        String thongBaoChung = "";
        Product product = auction.getProduct();

        synchronized (auction) {
            if ("COMPLETED".equals(auction.getStatus())) return;
            auction.setStatus("COMPLETED");

            if (product != null) {
                if (auction.getHighestBidder() != null) {
                    winnerEmail = auction.getHighestBidder().getEmail();
                    winnerName = auction.getHighestBidder().getUsername() != null ? auction.getHighestBidder().getUsername() : winnerEmail;
                    auction.setLeaderName(winnerName);

                    product.setStatus(ProductStatus.SOLD);
                    product.setCurrentPrice(finalPrice);

                    thongBaoChung = "Chúc mừng " + winnerName + " đã chốt đơn sản phẩm '" + product.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";
                    LOGGER.info("[AuctionManager] Sản phẩm " + product.getName() + " ĐÃ BÁN THÀNH CÔNG cho " + winnerEmail);

                    String sellerEmail = null;
                    if (product.getOwner() != null) {
                        if (product.getOwner().getEmail() != null && !product.getOwner().getEmail().trim().isEmpty()) {
                            sellerEmail = product.getOwner().getEmail();
                        } else if (product.getOwner().getId() != null) {
                            User sellerFromDB = UserDao.getInstance().findById(product.getOwner().getId());
                            if (sellerFromDB != null) sellerEmail = sellerFromDB.getEmail();
                        }
                    }

                    if (sellerEmail != null && !sellerEmail.trim().isEmpty()) {
                        UserDao.getInstance().depositMoney(sellerEmail, finalPrice);
                        PlaceBidHandler.updateClientBalance(context, gson, sellerEmail);

                        String thongBaoRieng = "Sản phẩm '" + product.getName() + "' của bạn đã bán thành công. Bạn nhận được: " + String.format("%,.0fđ", finalPrice);
                        Response sellerRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoRieng);
                        String sellerMsg = gson.toJson(sellerRes);

                        for (WebSocket client : context.getServer().getConnections()) {
                            if (sellerEmail.equals(context.getUserByConn(client)) && client.isOpen()) {
                                client.send(sellerMsg);
                                break;
                            }
                        }
                    }

                } else {
                    product.setStatus(ProductStatus.AVAILABLE);
                    finalPrice = product.getStartPrice();

                    thongBaoChung = "Rất tiếc, sản phẩm '" + product.getName() + "' đã hết giờ mà không có ai đặt giá!";
                    LOGGER.info("[AuctionManager] Sản phẩm " + product.getName() + " Ế HÀNG -> Trả về kho");

                    if (product.getOwner() != null) {
                        String sellerEmail = product.getOwner().getEmail();
                        if (sellerEmail != null && !sellerEmail.trim().isEmpty()) {
                            String thongBaoRieng = "Sản phẩm '" + product.getName() + "' của bạn đã kết thúc mà không có ai đặt giá.";
                            Response sellerRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoRieng);
                            String sellerMsg = gson.toJson(sellerRes);
                            for (WebSocket client : context.getServer().getConnections()) {
                                if (sellerEmail.equals(context.getUserByConn(client)) && client.isOpen()) {
                                    client.send(sellerMsg);
                                    break;
                                }
                            }
                        }
                    }
                }

                if (product.getStatus() != ProductStatus.SOLD) {
                    product.setStartTime(null);
                    product.setEndTime(null);
                }
                ProductDao.getInstance().editProduct(product);

                AuctionDao.getInstance().saveCompletedAuction(
                        String.valueOf(auction.getId()), product.getId(), winnerEmail, finalPrice
                );

                Response publicRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoChung);
                publicRes.getData().put("auction", auction);
                publicRes.getData().put("winnerName", winnerName);

                String publicMsg = gson.toJson(publicRes);
                for (WebSocket client : context.getServer().getConnections()) {
                    if (client.isOpen()) client.send(publicMsg);
                }
            }
        }

        // Giải phóng dữ liệu và dọn sạch hàng đợi Bot của RIÊNG phiên này
        context.removeAuction(auction.getId());
        botQueuesMap.remove(auction.getId());
    }
}