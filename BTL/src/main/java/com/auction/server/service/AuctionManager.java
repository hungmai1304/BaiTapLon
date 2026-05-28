package com.auction.server.service;

import com.auction.common.model.auction.Auction;
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

        List<Auction> pendingAuctions = context.getActiveAuctions().stream()
                .filter(a -> "PENDING".equals(a.getStatus()))
                .collect(Collectors.toList());

        if (pendingAuctions.isEmpty()) {
            System.out.println("[AuctionManager] Không còn phiên đấu giá PENDING nào để kích hoạt!");
            return null;
        }

        Auction selectedAuction = pendingAuctions.get(random.nextInt(pendingAuctions.size()));

        selectedAuction.setStatus("ACTIVE");
        selectedAuction.setStartTime(LocalDateTime.now());
        selectedAuction.setEndTime(LocalDateTime.now().plusMinutes(30));

        if (selectedAuction.getProduct() != null) {
            selectedAuction.getProduct().setStatus(ProductStatus.ON_AUCTION);
        }

        context.updateAuction(selectedAuction);

        System.out.println("[AuctionManager] Phiên đấu giá lên sàn ID: " + selectedAuction.getId());
        if (selectedAuction.getProduct() != null) {
            System.out.println("[AuctionManager] Sản phẩm lên sàn: " + selectedAuction.getProduct().getName());
        }

        broadcastNewAuction(selectedAuction);
        PlaceBidHandler.triggerBotWar(
                ServerContext.getInstance(), gson, selectedAuction.getProduct().getId(), selectedAuction
        );

        return selectedAuction;
    }

    private void broadcastNewAuction(Auction auction) {
        ServerContext context = ServerContext.getInstance();

        Response response = new Response(
                MessageType.GET_ACTIVE_AUCTIONS_RESPONSE,
                "SUCCESS",
                "Có phiên đấu giá mới vừa lên sàn!"
        );
        response.getData().put("auction", auction);

        String message = gson.toJson(response);

        for (WebSocket conn : context.getServer().getConnections()) {
            if (conn.isOpen()) {
                conn.send(message);
            }
        }
    }

    // ========== KẾT THÚC PHIÊN ĐẤU GIÁ (TRUNG TÂM XỬ LÝ) ==========
    public void endAuction(Auction auction) {
        if (auction == null) return;
        ServerContext context = ServerContext.getInstance();

        synchronized (auction) {
            // Ngăn chặn tuyệt đối việc luồng check expired và luồng scheduler cùng chạy song song
            if ("COMPLETED".equals(auction.getStatus())) return;
            auction.setStatus("COMPLETED");

            Product product = auction.getProduct();
            if (product != null) {
                String winnerEmail = null;
                String winnerName = "Không có";
                double finalPrice = auction.getCurrentPrice();
                String thongBaoChung = "";

                if (auction.getHighestBidder() != null) {
                    // =========================================================
                    // 1. TRƯỜNG HỢP CÓ NGƯỜI ĐẶT GIÁ
                    // =========================================================
                    winnerEmail = auction.getHighestBidder().getEmail();
                    winnerName = auction.getHighestBidder().getUsername() != null ? auction.getHighestBidder().getUsername() : winnerEmail;
                    auction.setLeaderName(winnerName);

                    // ĐỒNG BỘ CHECK SỐ DƯ NGƯỜI THẮNG TẠI ĐÂY (CHỐNG BÙNG KÈO)
                    User winnerUser = UserDao.getInstance().getUserByEmail(winnerEmail);
                    if (winnerUser != null && winnerUser.getBalance() >= finalPrice) {

                        // A. Trừ tiền người mua thành công
                        UserDao.getInstance().deductBalance(winnerEmail, finalPrice);
                        product.setStatus(ProductStatus.SOLD);
                        product.setCurrentPrice(finalPrice);

                        thongBaoChung = "Chúc mừng " + winnerName + " đã chốt đơn sản phẩm '" + product.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";
                        System.out.println("[AuctionManager] Sản phẩm " + product.getName() + " ĐÃ BÁN cho " + winnerEmail);

                        // B. Cộng tiền cho người bán (Seller)
                        String sellerEmail = null;
                        if (product.getOwner() != null) {
                            if (product.getOwner().getEmail() != null && !product.getOwner().getEmail().trim().isEmpty()) {
                                sellerEmail = product.getOwner().getEmail();
                            } else if (product.getOwner().getId() != null) {
                                User sellerFromDB = UserDao.getInstance().findById(product.getOwner().getId());
                                if (sellerFromDB != null) {
                                    sellerEmail = sellerFromDB.getEmail();
                                }
                            }
                        }

                        if (sellerEmail != null && !sellerEmail.trim().isEmpty()) {
                            UserDao.getInstance().depositMoney(sellerEmail, finalPrice);
                            PlaceBidHandler.updateClientBalance(context, gson, sellerEmail);

                            // Thông báo riêng tư cho Seller
                            String thongBaoRieng = "Sản phẩm '" + product.getName() + "' của bạn đã bán thành công. Bạn nhận được: " + String.format("%,.0fđ", finalPrice);
                            Response sellerRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoRieng);
                            String sellerMsg = gson.toJson(sellerRes);

                            for (WebSocket client : context.getServer().getConnections()) {
                                if (sellerEmail.equals(context.getUserByConn(client))) {
                                    if (client.isOpen()) client.send(sellerMsg);
                                    break;
                                }
                            }
                        }
                    } else {
                        // TRƯỜNG HỢP NGƯỜI THẮNG KHÔNG ĐỦ TIỀN (BÙNG KÈO)
                        product.setStatus(ProductStatus.AVAILABLE);
                        System.out.println("[AuctionManager] Đấu giá thất bại! Tài khoản " + winnerEmail + " bùng kèo SP " + product.getName());
                        thongBaoChung = "⚠ Rất tiếc, sản phẩm '" + product.getName() + "' đấu giá thất bại do người thắng không đủ số dư thanh toán!";
                        winnerEmail = null; // Đánh dấu không có người thắng vào DB hoàn tất
                    }

                } else {
                    // =========================================================
                    // 2. TRƯỜNG HỢP Ế HÀNG (KHÔNG AI ĐẶT GIÁ)
                    // =========================================================
                    product.setStatus(ProductStatus.AVAILABLE);
                    finalPrice = product.getStartPrice();

                    thongBaoChung = "Rất tiếc, sản phẩm '" + product.getName() + "' đã hết giờ mà không có ai chốt đơn!";
                    System.out.println("[AuctionManager] Sản phẩm " + product.getName() + " Ế HÀNG -> Trả về kho");

                    if (product.getOwner() != null) {
                        String sellerEmail = product.getOwner().getEmail();
                        if (sellerEmail != null && !sellerEmail.trim().isEmpty()) {
                            String thongBaoRieng = "Sản phẩm '" + product.getName() + "' của bạn đã kết thúc mà không có ai đặt giá.";
                            Response sellerRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoRieng);
                            String sellerMsg = gson.toJson(sellerRes);
                            for (WebSocket client : context.getServer().getConnections()) {
                                if (sellerEmail.equals(context.getUserByConn(client))) {
                                    if (client.isOpen()) client.send(sellerMsg);
                                    break;
                                }
                            }
                        }
                    }
                }

                // =========================================================
                // 3. ĐỒNG BỘ XUỐNG CƠ SỞ DỮ LIỆU & PHÁT LOA POPUP TOÀN MẠNG
                // =========================================================
                if (product.getStatus() != ProductStatus.SOLD) {
                    product.setStartTime(null);
                    product.setEndTime(null);
                }
                ProductDao.getInstance().editProduct(product);

                AuctionDao.getInstance().saveCompletedAuction(
                        String.valueOf(auction.getId()),
                        product.getId(),
                        winnerEmail,
                        finalPrice
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

        // DỌN DẸP RAM & CHUYỂN PHIÊN
        context.removeAuction(auction.getId());
        scheduleNextAuction();
    }

    private void scheduleNextAuction() {
        new Thread(() -> {
            try {
                System.out.println("[AuctionManager] Chờ 10 giây trước khi chọn phiên đấu giá mới...");
                Thread.sleep(10000);
                pickNextProduct();
            } catch (InterruptedException e) {
                System.err.println("[AuctionManager] Lỗi khi lên lịch: " + e.getMessage());
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public void checkAndEndExpiredAuctions() {
        ServerContext context = ServerContext.getInstance();
        List<Auction> activeAuctions = context.getActiveAuctions();

        synchronized (activeAuctions) {
            List<Auction> runningAuctions = new ArrayList<>(activeAuctions);
            for (Auction auction : runningAuctions) {
                if (auction != null &&
                        "ACTIVE".equals(auction.getStatus()) &&
                        LocalDateTime.now().isAfter(auction.getEndTime())) {

                    System.out.println("[AuctionManager] Phát hiện qua vòng lặp: Phiên ID " + auction.getId() + " ĐÃ HẾT GIỜ!");
                    endAuction(auction);
                }
            }
        }
    }
}