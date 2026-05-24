package com.auction.server.service;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.model.ServerContext;
import com.auction.server.dao.ProductDao;
import com.auction.server.dao.AuctionDao;
import com.auction.common.utils.LocalDateTimeAdapter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AuctionManager {

    // ========== SINGLETON PATTERN ==========
    private static AuctionManager instance;
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();
    private final Random random = new Random();

    private AuctionManager() {}

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // ========== LOGIC CHỌN PHIÊN ĐẤU GIÁ LÊN SÀN ==========

    public Auction pickNextProduct() {
        ServerContext context = ServerContext.getInstance();

        // 1. Lọc danh sách các phiên đấu giá đang ở trạng thái PENDING từ RAM
        List<Auction> pendingAuctions = context.getActiveAuctions().stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .collect(Collectors.toList());

        if (pendingAuctions.isEmpty()) {
            System.out.println("[AuctionManager] Không còn phiên đấu giá PENDING nào để kích hoạt!");
            return null;
        }

        // 2. Chọn ngẫu nhiên 1 phiên đấu giá chuẩn bị mở
        Auction selectedAuction = pendingAuctions.get(random.nextInt(pendingAuctions.size()));

        // 3. Cập nhật trạng thái của phiên đấu giá thành ACTIVE và set thời gian chạy sàn
        selectedAuction.setStatus("ACTIVE");
        selectedAuction.setStartTime(LocalDateTime.now());
        selectedAuction.setEndTime(LocalDateTime.now().plusMinutes(30)); // Giả sử mỗi phiên kéo dài 30 phút

        // Đồng thời cập nhật trạng thái của Product nằm trong phiên đấu giá đó sang ON_AUCTION
        if (selectedAuction.getProduct() != null) {
            selectedAuction.getProduct().setStatus(ProductStatus.ON_AUCTION);
        }

        // 4. Đồng bộ cập nhật lại phiên đấu giá này vào ServerContext trên RAM
        context.updateAuction(selectedAuction);

        System.out.println("[AuctionManager] Phiên đấu giá lên sàn ID: " + selectedAuction.getId());
        if (selectedAuction.getProduct() != null) {
            System.out.println("[AuctionManager] Sản phẩm lên sàn: " + selectedAuction.getProduct().getName());
        }
        System.out.println("[AuctionManager] Kết thúc lúc: " + selectedAuction.getEndTime());

        // 5. Broadcast thông báo cho tất cả client
        broadcastNewAuction(selectedAuction);

        return selectedAuction;
    }

    // ========== BROADCAST CHO TẤT CẢ CLIENT ==========

    /**
     * Thông báo cho tất cả client về phiên đấu giá mới vừa hoạt động
     */
    private void broadcastNewAuction(Auction auction) {
        ServerContext context = ServerContext.getInstance();

        // Tạo response chứa đối tượng đấu giá Auction vừa kích hoạt
        Response response = new Response(
                MessageType.GET_ACTIVE_AUCTIONS_RESPONSE,
                "SUCCESS",
                "Có phiên đấu giá mới vừa lên sàn!"
        );
        response.getData().put("auction", auction);

        String message = gson.toJson(response);

        // Gửi cho tất cả client đang online
        for (WebSocket conn : context.getServer().getConnections()) {
            if (conn.isOpen()) {
                conn.send(message);
            }
        }

        System.out.println("[AuctionManager] Đã broadcast phiên đấu giá mới cho " +
                context.getServer().getConnections().size() + " client!");
    }

    // ========== KẾT THÚC PHIÊN ĐẤU GIÁ ==========

    /**
     * Kết thúc phiên đấu giá hiện tại (Thread-safe)
     * @param auction Phiên đấu giá cần kết thúc
     */
    public void endAuction(Auction auction) {
        if (auction == null) return;

        synchronized (auction) {
            // Kiểm tra lại trạng thái để tránh chốt đơn 2 lần (Double closing)
            if ("COMPLETED".equals(auction.getStatus())) {
                return;
            }

            ServerContext context = ServerContext.getInstance();
            auction.setStatus("COMPLETED");

            Product product = auction.getProduct();
            if (product != null) {
                String winnerEmail = null;
                double finalPrice = auction.getCurrentPrice();

                if (auction.getHighestBidder() != null) {
                    winnerEmail = auction.getHighestBidder().getEmail();
                    product.setStatus(ProductStatus.SOLD);
                    product.setCurrentPrice(finalPrice);

                    System.out.println("[CHỐT ĐƠN] SP: " + product.getName() + " | Winner: " + winnerEmail + " | Giá: " + finalPrice);
                    String thongBao = "🎉 Chúc mừng " + winnerEmail + " đã đấu giá thành công '" + product.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";
                    broadcastAuctionResultNotification(context, thongBao);
                } else {
                    product.setStatus(ProductStatus.AVAILABLE);
                    product.setStartTime(null);
                    product.setEndTime(null);
                    finalPrice = product.getStartPrice();

                    System.out.println("[CHỐT ĐƠN] SP: " + product.getName() + " | KHÔNG CÓ NGƯỜI MUA");
                    String thongBao = "Rất tiếc, phiên đấu giá '" + product.getName() + "' đã kết thúc mà không có ai đặt lệnh!";
                    broadcastAuctionResultNotification(context, thongBao);
                }

                // Lưu Database
                ProductDao.getInstance().editProduct(product);
                AuctionDao.getInstance().saveCompletedAuction(auction.getId(), product.getId(), winnerEmail, finalPrice);
            }

            // Xóa khỏi RAM và Broadcast update
            context.removeAuction(auction.getId());
            broadcastAuctionEnd(auction);
        }

        // Chọn phiên đấu giá tiếp theo (sau 10 giây)
        scheduleNextAuction();
    }

    private void broadcastAuctionResultNotification(ServerContext context, String thongBao) {
        Response res = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBao);
        String msg = gson.toJson(res);
        for (WebSocket client : context.getServer().getConnections()) {
            if (client.isOpen()) client.send(msg);
        }
    }

    /**
     * Broadcast thông báo kết thúc phiên đấu giá
     */
    private void broadcastAuctionEnd(Auction auction) {
        ServerContext context = ServerContext.getInstance();

        Response response = new Response(
                "AUCTION_END",
                "SUCCESS",
                "Phiên đấu giá kết thúc!"
        );
        response.getData().put("auction", auction);

        String message = gson.toJson(response);

        for (WebSocket conn : context.getServer().getConnections()) {
            if (conn.isOpen()) {
                conn.send(message);
            }
        }

        System.out.println("[AuctionManager] Đã broadcast kết thúc phiên đấu giá ID: " + auction.getId());
    }

    /**
     * Lên lịch chọn phiên đấu giá kế tiếp sau 10 giây
     */
    private void scheduleNextAuction() {
        new Thread(() -> {
            try {
                System.out.println("[AuctionManager] Chờ 10 giây trước khi chọn phiên đấu giá mới...");
                Thread.sleep(10000); // 10 giây
                pickNextProduct();
            } catch (InterruptedException e) {
                System.err.println("[AuctionManager] Lỗi khi lên lịch: " + e.getMessage());
            }
        }).start();
    }

    // ========== TỰ ĐỘNG KẾT THÚC KHI HẾT GIỜ ==========

    /**
     * Kiểm tra và tự động kết thúc tất cả các phiên đấu giá đã quá giờ chạy sàn
     * Gọi định kỳ mỗi 1 giây từ vòng lặp Server
     */
    public void checkAndEndExpiredAuctions() {
        ServerContext context = ServerContext.getInstance();
        List<Auction> activeAuctions = context.getActiveAuctions();

        // Sử dụng một bản sao danh sách để tránh lỗi ConcurrentModificationException
        synchronized (activeAuctions) {
            List<Auction> runningAuctions = new ArrayList<>(activeAuctions);
            for (Auction auction : runningAuctions) {
                if (auction != null &&
                        "ACTIVE".equals(auction.getStatus()) &&
                        LocalDateTime.now().isAfter(auction.getEndTime())) {

                    System.out.println("[AuctionManager] Phiên đấu giá ID " + auction.getId() + " ĐÃ HẾT GIỜ!");
                    endAuction(auction);
                }
            }
        }
    }
}