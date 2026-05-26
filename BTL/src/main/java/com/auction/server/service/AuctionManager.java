package com.auction.server.service;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
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

import static com.auction.server.handler.bidding.PlaceBidHandler.updateClientBalance;

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

    /**
     * Kết thúc phiên đấu giá hiện tại
     * @param auction Phiên đấu giá cần kết thúc
     */
    // lỗi ở đây
    // ========== KẾT THÚC PHIÊN ĐẤU GIÁ ==========

    public void endAuction(Auction auction) {
        if (auction == null) return;

        ServerContext context = ServerContext.getInstance();
        auction.setStatus("COMPLETED");

        Product product = auction.getProduct();
        if (product != null) {
            String winnerEmail = null;
            String winnerName = "Không có"; // Khởi tạo mặc định tránh lỗi null
            double finalPrice = auction.getCurrentPrice();
            String thongBaoChung = "";

            if (auction.getHighestBidder() != null) {
                // 1. TRƯỜNG HỢP CÓ NGƯỜI MUA (CHỐT ĐƠN)

                winnerEmail = auction.getHighestBidder().getEmail();
                // FIX LỖI TÊN "Không có": Lấy Username, nếu trống thì lấy Email
                winnerName = auction.getHighestBidder().getUsername() != null ? auction.getHighestBidder().getUsername() : winnerEmail;
                auction.setLeaderName(winnerName);

                product.setStatus(ProductStatus.SOLD);
                product.setCurrentPrice(finalPrice);

                thongBaoChung = "Chúc mừng " + winnerName + " đã chốt đơn sản phẩm '" + product.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";
                System.out.println("[AuctionManager] Sản phẩm " + product.getName() + " ĐÃ BÁN cho " + winnerEmail);


                // XỬ LÝ RIÊNG TƯ CHO NGƯỜI BÁN (SELLER)

                if (product.getOwner() != null) {
                    String sellerEmail = product.getOwner().getEmail();

                    // A. Cộng tiền vào Database cho Seller
                    UserDao.getInstance().depositMoney(sellerEmail, finalPrice);
                    System.out.println("💰 [Payout] Đã chuyển " + String.format("%,.0fđ", finalPrice) + " cho Seller: " + sellerEmail);

                    // B. Nháy số dư trên màn hình Seller
                    PlaceBidHandler.updateClientBalance(context, gson, sellerEmail);

                    // C. Đóng gói thông báo RIÊNG TƯ chỉ dành cho Seller
                    String thongBaoRieng = "Sản phẩm '" + product.getName() + "' của bạn đã bán thành công cho " + winnerName + ". Nhận được: " + String.format("%,.0fđ", finalPrice);
                    Response sellerRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoRieng);
                    String sellerMsg = gson.toJson(sellerRes);

                    // D. Truy tìm đúng đường truyền của Seller để gửi thông báo mật
                    for (WebSocket client : context.getServer().getConnections()) {
                        if (sellerEmail.equals(context.getUserByConn(client))) {
                            if (client.isOpen()) client.send(sellerMsg);
                            break; // Gửi xong là thoát vòng lặp luôn
                        }
                    }
                }

            } else {
                // 2. TRƯỜNG HỢP Ế HÀNG
                
                product.setStatus(ProductStatus.AVAILABLE);
                finalPrice = product.getStartPrice();

                thongBaoChung = "Rất tiếc, sản phẩm '" + product.getName() + "' đã hết giờ mà không có ai chốt đơn!";
                System.out.println("[AuctionManager] Sản phẩm " + product.getName() + " Ế HÀNG -> Hủy bỏ");

                // (Tùy chọn) Gửi thông báo ế hàng riêng cho Seller
                if (product.getOwner() != null) {
                    String sellerEmail = product.getOwner().getEmail();
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

            // 3. LƯU TRẠNG THÁI SẢN PHẨM XUỐNG DB
            product.setStartTime(null);
            product.setEndTime(null);
            ProductDao.getInstance().editProduct(product);

            // 4. LƯU VÀO SỔ CÁI ĐẤU GIÁ
            AuctionDao.getInstance().saveCompletedAuction(
                    String.valueOf(auction.getId()),
                    product.getId(),
                    winnerEmail,
                    finalPrice
            );


            // 5. PHÁT LOA CHUNG CHO CẢ SÀN (Kèm theo tên người thắng)

            Response publicRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoChung);
            publicRes.getData().put("auction", auction);
            publicRes.getData().put("winnerName", winnerName); // Đóng gói tên để Client đọc

            String publicMsg = gson.toJson(publicRes);
            for (WebSocket client : context.getServer().getConnections()) {
                if (client.isOpen()) client.send(publicMsg);
            }
        }

        // 6. DỌN DẸP RAM
        context.removeAuction(auction.getId());

        // Chọn phiên tiếp theo
        scheduleNextAuction();
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