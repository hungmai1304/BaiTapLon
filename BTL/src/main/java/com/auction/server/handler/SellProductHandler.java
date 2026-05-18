package com.auction.server.handler;

import com.auction.common.model.product.Item;
import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.google.gson.*;
import java.time.format.DateTimeFormatter;

import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import java.util.Map;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandMap(value = MessageType.SELL_PRODUCT_REQUEST)
public class SellProductHandler implements IMessageHandler {

    // TẠO MỘT BỘ ĐẾM GIỜ CHẠY NGẦM ĐỘC LẬP
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // TẠO SAFEGSON (Chuyên xử lý thời gian LocalDateTime)
    private static final Gson safeGson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("id");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, safeGson, "Không tìm thấy mã sản phẩm để lên sàn!");
                return;
            }

            // 1. Cập nhật trạng thái sản phẩm trong Database thành đang đem bán đấu giá
            boolean isSold = ProductDao.getInstance().sellProduct(productId);

            if (isSold) {
                // 2. Lấy bản mới nhất của Product từ DB lên để đóng gói vào Auction
                Product updatedProduct = ProductDao.getInstance().getProductById(productId);

                if (updatedProduct != null) {
                    // BẮT ĐẦU LOGIC TẠO PHIÊN ĐẤU GIÁ
                    int auctionId = Math.abs(new Random().nextInt()); // Tạo ID tự động cho phiên

                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startTime = now.plusSeconds(30);
                    LocalDateTime endTime = startTime.plusSeconds(15);

                    // Đóng gói thành Phiên Đấu Giá (Gắn product trực tiếp vào đây)
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

                    // Cất thẳng vào kho chứa Auction trên RAM (Bỏ qua lưu product độc lập trên RAM)
                    context.addAuction(newAuction);
                }

                // PHẦN HẸN GIỜ TỰ ĐỘNG CHUYỂN TRẠNG THÁI PHIÊN ĐẤU GIÁ
                // =========================================================
                long delayToActive = 30;    // Chờ 30s để bắt đầu (Lúc TEST đổi từ phút thành giây)
                long delayToCompleted = 45; // Tổng 45s để kết thúc

                // Hẹn giờ 1: MỞ BÁT PHIÊN ĐẤU GIÁ (PENDING -> ACTIVE)
                scheduler.schedule(() -> {
                    Auction auctionToStart = context.getAuctionByProductId(productId);
                    if (auctionToStart != null && "PENDING".equals(auctionToStart.getStatus())) {
                        auctionToStart.setStatus("ACTIVE");
                        context.updateAuction(auctionToStart); // Hàm này tự động broadcast update danh sách phiên
                        System.out.println(" [Timer] SP " + productId + " ĐÃ LÊN SÀN ĐẤU GIÁ!");
                    }
                }, delayToActive, TimeUnit.SECONDS);

                // Hẹn giờ 2: KHÓA SỔ PHIÊN ĐẤU GIÁ & HẠ SẢN PHẨM (ACTIVE -> COMPLETED)
                scheduler.schedule(() -> {
                    Auction auctionToEnd = context.getAuctionByProductId(productId);
                    if (auctionToEnd != null && !"COMPLETED".equals(auctionToEnd.getStatus())) {

                        // A. Chốt phiên đấu giá thành COMPLETED trên RAM
                        auctionToEnd.setStatus("COMPLETED");
                        context.updateAuction(auctionToEnd);

                        // B. Lấy thông tin sản phẩm gắn liền trong phiên để khôi phục trạng thái dưới DB
                        Product p = auctionToEnd.getProduct();
                        if (p != null) {
                            p.setStatus(ProductStatus.AVAILABLE);
                            p.setStartTime(null);
                            p.setEndTime(null);

                            // Lưu trực tiếp xuống Database. Do RAM không giữ productList nữa nên không cần update RAM cho Product.
                            ProductDao.getInstance().editProduct(p);
                        }

                        // C. Thông báo danh sách phiên đấu giá mới nhất (Đã cập nhật trạng thái COMPLETED) cho Client
                        broadcastNewAuctionSession(context, safeGson);
                        System.out.println("[Timer] SP " + productId + " ĐÃ HẾT GIỜ. Phiên kết thúc!");
                    }
                }, delayToCompleted, TimeUnit.SECONDS);

                // 3. Phản hồi thành công cho Seller đang gửi request
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá!");
                conn.send(safeGson.toJson(response));

                System.out.println("-> [SellProduct] Thành công: ID " + productId);

                // 4. Phát loa thông báo danh sách phiên đấu giá mới (chứa phiên PENDING vừa tạo) cho toàn bộ Client đang online
                broadcastNewAuctionSession(context, safeGson);

            } else {
                sendError(conn, safeGson, "Lỗi khi cập nhật trạng thái lên Database!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, safeGson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    // HÀM PHÁT LOA ĐỂ BÁO CÁO DANH SÁCH AUCTION MỚI NHẤT
    private void broadcastNewAuctionSession(ServerContext context, Gson gson) {
        List<Auction> activeAuctions = context.getActiveAuctions();

        Response updateRes = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Có phiên đấu giá mới được cập nhật!");
        updateRes.getData().put("auctionList", activeAuctions);

        String message = safeGson.toJson(updateRes);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
        System.out.println("-> [Broadcast] Đã phát sóng danh sách Phiên Đấu Giá mới tới toàn bộ Client.");
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(safeGson.toJson(response));
    }
}