package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime; // import để tính giờ
import java.util.Random; //  import để random ID
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandMap(value = MessageType.SELL_PRODUCT_REQUEST)
public class SellProductHandler implements IMessageHandler {
    // TẠO MỘT BỘ ĐẾM GIỜ CHẠY NGẦM ĐỘC LẬP (Không làm đơ logic cũ)
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);


    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("id");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, gson, "Không tìm thấy mã sản phẩm để lên sàn!");
                return;
            }

            // 1. Cập nhật trạng thái trong Database
            boolean isSold = ProductDao.getInstance().sellProduct(productId);

            if (isSold) {
                // 2. Cập nhật RAM: Lấy bản mới nhất từ DB
                Product updatedProduct = ProductDao.getInstance().getProductById(productId);
                if (updatedProduct != null) {
                    context.updateProduct(updatedProduct);
                    //  BẮT ĐẦU LOGIC TẠO PHIÊN ĐẤU GIÁ
                    int auctionId = Math.abs(new Random().nextInt()); // Tạo ID tự động

                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startTime = now.plusMinutes(30);
                    LocalDateTime endTime = startTime.plusMinutes(10);

                    // Đóng gói thành Phiên Đấu Giá
                    Auction newAuction = new Auction(
                            auctionId,
                            updatedProduct,
                            updatedProduct.getStartPrice(),
                            updatedProduct.getStepPrice(),
                            updatedProduct.getStartPrice(),
                            startTime,
                            endTime
                    );
                    newAuction.setStatus("PENDING");

                    // Cất vào kho chứa Auction trên RAM
                    context.addAuction(newAuction);
                }

                // PHẦN MỚI THÊM: HẸN GIỜ TỰ ĐỘNG CHUYỂN TRẠNG THÁI
                // =========================================================
                long delayToActive = 30; // Chờ 30p để bắt đầu
                long delayToCompleted = 40; // Tổng 40p để kết thúc

                // Hẹn giờ 1: MỞ BÁT (PENDING -> ACTIVE)
                scheduler.schedule(() -> {
                    Auction auctionToStart = context.getAuctionByProductId(productId);
                    if (auctionToStart != null && "PENDING".equals(auctionToStart.getStatus())) {
                        auctionToStart.setStatus("ACTIVE");
                        context.updateAuction(auctionToStart);
                        System.out.println(" [Timer] SP " + productId + " đã lên sàn ĐẤU GIÁ!");
                    }
                }, delayToActive, TimeUnit.SECONDS); // ⚠️ LÚC TEST ĐỔI THÀNH TimeUnit.SECONDS

                // Hẹn giờ 2: KHÓA SỔ VÀ TRẢ ĐỒ VỀ KHO (ACTIVE -> COMPLETED)
                scheduler.schedule(() -> {
                    Auction auctionToEnd = context.getAuctionByProductId(productId);
                    if (auctionToEnd != null && !"COMPLETED".equals(auctionToEnd.getStatus())) {

                        // A. Chốt phiên đấu giá
                        auctionToEnd.setStatus("COMPLETED");
                        context.updateAuction(auctionToEnd);

                        // B. Kéo sản phẩm về trạng thái có sẵn để "My Shop" bình thường lại
                        Product p = context.getProductById(productId);
                        if (p != null) {
                            // Ghi chú: Nếu trạng thái mặc định của anh là IDLE thì đổi chữ AVAILABLE thành IDLE nhé
                            p.setStatus(ProductStatus.AVAILABLE);
                            ProductDao.getInstance().editProduct(p); // Lưu xuống DB
                            context.updateProduct(p); // Cập nhật RAM

                            // C. Gọi lại hàm Broadcast CŨ của anh để báo Client tải lại My Shop
                            broadcastNewList(context, gson);
                        }
                        System.out.println("[Timer] SP " + productId + " đã HẾT GIỜ. Trả về kho!");
                    }
                }, delayToCompleted, TimeUnit.SECONDS); // ⚠ LÚC TEST ĐỔI THÀNH TimeUnit.SECONDS

                // 3. Phản hồi cho thằng Seller
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá!");
                conn.send(gson.toJson(response));

                System.out.println("-> [SellProduct] Thành công: ID " + productId);

                // 4. Phát loa thông báo cho tất cả mọi người
                broadcastNewList(context, gson);
                // 5. Phát loa thông báo phiên đấu giá mới (Dành cho giao diện đấu giá sắp làm)
                broadcastNewAuctionSession(context, gson);

            } else {
                sendError(conn, gson, "Lỗi khi cập nhật trạng thái lên Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void broadcastNewList(ServerContext context, Gson gson) {
        // Lấy danh sách sản phẩm từ RAM
        List<Product> listToSend = context.getProductList();

        // --- BỎ HẾT CÁI VÒNG LẶP ĐỌC FILE Ở ĐÂY ---
        // Vì imagePath đã là link URL rồi, Client chỉ việc cầm link đó mà hiển thị thôi.

        Response updateRes = new Response(MessageType.UPDATE_AUCTION_LIST_RESPONSE, "SUCCESS", "Sàn vừa có món mới!");
        updateRes.getData().put("productList", listToSend);

        String message = gson.toJson(updateRes);

        // Gửi cho tất cả mọi người đang online
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }

        // --- BỎ LUÔN CÁI BƯỚC set null Base64 giải phóng RAM ---
        // Vì nãy giờ mình có nạp cái gì nặng vào đâu mà cần giải phóng.
        System.out.println("-> [Broadcast] Đã cập nhật danh sách đấu giá mới cho tất cả Client qua URL Cloudinary.");
    }


    //  HÀM NÀY ĐỂ BÁO CÁO DANH SÁCH AUCTION MỚI
    private void broadcastNewAuctionSession(ServerContext context, Gson gson) {
        List<Auction> activeAuctions = context.getActiveAuctions();

        // Dùng một MessageType tên khác (UPDATE_ACTIVE_AUCTIONS_RESPONSE) để tránh đụng chạm
        Response updateRes = new Response("UPDATE_ACTIVE_AUCTIONS_RESPONSE", "SUCCESS", "Có phiên đấu giá mới được lên lịch!");
        updateRes.getData().put("auctionList", activeAuctions);

        String message = gson.toJson(updateRes);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
        System.out.println("-> [Broadcast MỚI] Đã phát sóng danh sách Phiên Đấu Giá.");
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}