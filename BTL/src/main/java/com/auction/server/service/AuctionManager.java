package com.auction.server.service;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class AuctionManager {

    // ========== SINGLETON PATTERN ==========
    private static AuctionManager instance;
    private final Gson gson = new Gson();
    private final Random random = new Random();

    private AuctionManager() {}

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    // ========== LOGIC CHỌN SẢN PHẨM LÊN ĐẤU GIÁ ==========

    /**
     * Chọn 1 sản phẩm AVAILABLE ngẫu nhiên và đưa lên đấu giá
     * return Product được chọn, hoặc null nếu không còn sản phẩm nào
     */
    public Product pickNextProduct() {
        ServerContext context = ServerContext.getInstance();

        // 1. Lấy danh sách sản phẩm AVAILABLE
        List<Product> availableProducts = context.getProductList().stream()
                .filter(p -> p.getStatus() == ProductStatus.AVAILABLE)
                .collect(Collectors.toList());

        if (availableProducts.isEmpty()) {
            System.out.println("[AuctionManager] ⚠️ Không còn sản phẩm nào để đấu giá!");
            return null;
        }

        // 2. Random chọn 1 sản phẩm
        Product selectedProduct = availableProducts.get(random.nextInt(availableProducts.size()));

        // 3. Đổi status → ON_AUCTION
        selectedProduct.setStatus(ProductStatus.ON_AUCTION);
        selectedProduct.setStartTime(LocalDateTime.now());
        // Giả sử mỗi phiên kéo dài 5 phút
        selectedProduct.setEndTime(LocalDateTime.now().plusMinutes(30));

        // 4. Lưu vào ServerContext
        context.setCurrentProduct(selectedProduct);

        System.out.println("🔴 [AuctionManager] Sản phẩm lên sàn: " + selectedProduct.getName());
        System.out.println("   ⏰ Kết thúc lúc: " + selectedProduct.getEndTime());

        // 5. Broadcast cho tất cả client
        broadcastNewAuction(selectedProduct);

        return selectedProduct;
    }

    // ========== BROADCAST CHO TẤT CẢ CLIENT ==========

    /**
     * Thông báo cho tất cả client về sản phẩm mới lên đấu giá
     */
    private void broadcastNewAuction(Product product) {
        ServerContext context = ServerContext.getInstance();

        // Tạo response
        Response response = new Response(
                MessageType.GET_AUCTION_PRODUCT_RESPONSE,
                "SUCCESS",
                "Sản phẩm mới lên đấu giá: " + product.getName()
        );
        response.getData().put("product", product);

        String message = gson.toJson(response);

        // Gửi cho tất cả client đang online
        for (WebSocket conn : context.getServer().getConnections()) {
            if (conn.isOpen()) {
                conn.send(message);
            }
        }

        System.out.println("📡 [AuctionManager] Đã broadcast sản phẩm mới cho " +
                context.getServer().getConnections().size() + " client!");
    }

    // ========== KẾT THÚC PHIÊN ĐẤU GIÁ ==========

    /**
     * Kết thúc phiên đấu giá hiện tại
     * @param product Sản phẩm cần kết thúc
     */
    public void endAuction(Product product) {
        if (product == null) return;

        // Đổi status → SOLD (hoặc CANCELLED nếu không ai đấu)
        if (product.getCurrentPrice() > product.getStartPrice()) {
            product.setStatus(ProductStatus.SOLD);
            System.out.println("✅ [AuctionManager] Sản phẩm " + product.getName() +
                    " đã BÁN với giá: " + product.getCurrentPrice());
        } else {
            product.setStatus(ProductStatus.CANCELLED);
            System.out.println("❌ [AuctionManager] Sản phẩm " + product.getName() +
                    " KHÔNG CÓ AI ĐẤU GIÁ → Hủy bỏ");
        }

        // Broadcast kết quả
        broadcastAuctionEnd(product);

        // Chọn sản phẩm tiếp theo (sau 10 giây)
        scheduleNextAuction();
    }

    /**
     * Broadcast thông báo kết thúc phiên đấu giá
     */
    private void broadcastAuctionEnd(Product product) {
        ServerContext context = ServerContext.getInstance();

        Response response = new Response(
                "AUCTION_END",
                "SUCCESS",
                "Phiên đấu giá kết thúc!"
        );
        response.getData().put("product", product);

        String message = gson.toJson(response);

        for (WebSocket conn : context.getServer().getConnections()) {
            if (conn.isOpen()) {
                conn.send(message);
            }
        }

        System.out.println("📡 [AuctionManager] Đã broadcast kết thúc phiên!");
    }

    /**
     * Lên lịch chọn sản phẩm kế tiếp sau 10 giây
     */
    private void scheduleNextAuction() {
        new Thread(() -> {
            try {
                System.out.println("⏳ [AuctionManager] Chờ 10 giây trước khi chọn sản phẩm mới...");
                Thread.sleep(10000); // 10 giây
                pickNextProduct();
            } catch (InterruptedException e) {
                System.err.println("❌ [AuctionManager] Lỗi khi lên lịch: " + e.getMessage());
            }
        }).start();
    }

    // ========== TỰ ĐỘNG KẾT THÚC KHI HẾT GIỜ ==========

    /**
     * Kiểm tra và tự động kết thúc phiên nếu hết giờ
     * Gọi định kỳ mỗi 1 giây từ Server
     */
    public void checkAndEndExpiredAuctions() {
        ServerContext context = ServerContext.getInstance();
        Product current = context.getCurrentProduct();

        if (current != null &&
                current.getStatus() == ProductStatus.ON_AUCTION &&
                LocalDateTime.now().isAfter(current.getEndTime())) {

            System.out.println("⏰ [AuctionManager] Phiên đấu giá HẾT GIỜ!");
            endAuction(current);
        }
    }
}