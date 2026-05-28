package com.auction.server.handler.shop;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.auction.server.service.AuctionManager;
import com.google.gson.*;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandMap(value = MessageType.SELL_PRODUCT_REQUEST)
public class SellProductHandler implements IMessageHandler {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    private static final double DEFAULT_TIME_WAITING_TO_START_MINUTES = 1.0;
    private static final double DEFAULT_TIME_AUCTION_DURATION_MINUTES = 2.0;

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
                sendError(conn, "Không tìm thấy mã sản phẩm để lên sàn!");
                return;
            }

            Product pCheck = ProductDao.getInstance().getProductById(productId);
            if (pCheck == null) {
                sendError(conn, "Sản phẩm không tồn tại trong hệ thống!");
                return;
            }

            if (pCheck.getStatus() == ProductStatus.SOLD) {
                sendError(conn, "Thất bại: Sản phẩm này đã ĐƯỢC CHỐT ĐƠN!");
                return;
            }

            if (pCheck.getStatus() == ProductStatus.NOT_AVAILABLE) {
                sendError(conn, "Thất bại: Sản phẩm này đang ở trạng thái KHÔNG KHẢ DỤNG (Bị khóa)!");
                return;
            }

            if (pCheck.getStatus() == ProductStatus.ON_AUCTION) {
                sendError(conn, "Thất bại: Sản phẩm này đang trong một phiên đấu giá khác rồi!");
                return;
            }

            // =========================================================================
            // BẢN FIX BỌC THÉP: KIỂM TRA ĐA DẠNG KEY & CHẤP MỌI KIỂU DỮ LIỆU TỪ CLIENT
            // =========================================================================
            Object waitingObj = data.get("waitingMinutes") != null ? data.get("waitingMinutes") : data.get("waiting_minutes");
            Object durationObj = data.get("durationMinutes") != null ? data.get("durationMinutes") : data.get("duration_minutes");
            Object startPriceObj = data.get("startPrice") != null ? data.get("startPrice") : data.get("start_price");

            // Tiến hành parse an toàn bằng hàm tiện ích tự dựng phía dưới
            double waitingMinutes = safeParseDouble(waitingObj, DEFAULT_TIME_WAITING_TO_START_MINUTES);
            double durationMinutes = safeParseDouble(durationObj, DEFAULT_TIME_AUCTION_DURATION_MINUTES);
            double liveStartPrice = safeParseDouble(startPriceObj, pCheck.getStartPrice());

            System.out.println("[SellProductHandler] Nhận từ client -> Chờ sàn: " + waitingMinutes + " phút | Chạy sàn: " + durationMinutes + " phút | Giá khởi điểm: " + liveStartPrice);

            LocalDateTime now = LocalDateTime.now();

            // Tính toán thời gian dựa trên số giây quy đổi chính xác tuyệt đối
            long waitingSeconds = Math.round(waitingMinutes * 60);
            long durationSeconds = Math.round(durationMinutes * 60);

            LocalDateTime startTime = now.plusSeconds(waitingSeconds);
            LocalDateTime endTime = startTime.plusSeconds(durationSeconds);

            boolean isUpdated = ProductDao.getInstance().sellProduct(productId, liveStartPrice, startTime, endTime);

            if (isUpdated) {
                pCheck.setStatus(ProductStatus.ON_AUCTION);
                pCheck.setStartPrice(liveStartPrice);
                pCheck.setCurrentPrice(liveStartPrice);
                pCheck.setStartTime(startTime);
                pCheck.setEndTime(endTime);

                String auctionId = UUID.randomUUID().toString();

                Auction newAuction = new Auction(
                        pCheck,
                        pCheck.getStartPrice(),
                        pCheck.getStepPrice(),
                        pCheck.getStartPrice(),
                        startTime,
                        endTime
                );
                newAuction.setStatus("PENDING");
                newAuction.setId(auctionId);

                context.addAuction(newAuction);

                long delayToActiveSeconds = waitingSeconds;
                long delayToCompletedSeconds = waitingSeconds + durationSeconds;

                // HẸN GIỜ 1: KÍCH HOẠT PHIÊN (PENDING -> ACTIVE)
                scheduler.schedule(() -> {
                    try {
                        Auction auctionToStart = context.getAuctionByProductId(productId);
                        if (auctionToStart != null && "PENDING".equals(auctionToStart.getStatus())) {
                            auctionToStart.setStatus("ACTIVE");
                            context.updateAuction(auctionToStart);
                            System.out.println(" [Timer] SP " + productId + " đã CHÍNH THỨC LÊN SÀN ĐẤU GIÁ!");
                            broadcastNewAuctionSession(context);
                        }
                    } catch (Exception e) {
                        System.err.println("[Timer Error] Lỗi khi kích hoạt phiên: " + e.getMessage());
                    }
                }, delayToActiveSeconds, TimeUnit.SECONDS);

                // HẸN GIỜ 2: ĐÓNG PHIÊN VÀ CHỐT ĐƠN (GỌI BỘ NÃO TRUNG TÂM)
                scheduler.schedule(() -> {
                    try {
                        Auction auctionToEnd = context.getAuctionByProductId(productId);
                        if (auctionToEnd != null) {
                            AuctionManager.getInstance().endAuction(auctionToEnd);
                            broadcastNewAuctionSession(context);
                            broadcastToAdmins(context);
                        }
                    } catch (Exception e) {
                        System.err.println("[Timer Error] Lỗi khi kết thúc phiên hẹn giờ: " + e.getMessage());
                    }
                }, delayToCompletedSeconds, TimeUnit.SECONDS);

                // Phản hồi cho người bán gửi request thành công
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá thành công!");
                conn.send(safeGson.toJson(response));

                broadcastNewAuctionSession(context);
                broadcastToAdmins(context);

            } else {
                sendError(conn, "Lỗi khi cập nhật trạng thái lên Database!");
            }

        } catch (Exception e) {
            System.err.println("[SellProductHandler] Lỗi hệ thống: " + e.getMessage());
            sendError(conn, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * HÀM TIỆN ÍCH: ÉP KIỂU AN TOÀN TUYỆT ĐỐI CHỐNG LỖI ĐỊNH DẠNG DỮ LIỆU TỪ CLIENT
     */
    private double safeParseDouble(Object value, double defaultValue) {
        if (value == null) return defaultValue;
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble(((String) value).trim());
            }
        } catch (Exception e) {
            System.err.println("[Format Warn] Không thể ép kiểu giá trị '" + value + "' sang Double. Dùng mặc định: " + defaultValue);
        }
        return defaultValue;
    }

    private void broadcastNewAuctionSession(ServerContext context) {
        List<Auction> activeAuctions = context.getActiveAuctions();

        Response updateRes = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Có phiên đấu giá mới được cập nhật!");
        updateRes.getData().put("auctionList", activeAuctions);

        String message = safeGson.toJson(updateRes);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(message);
            }
        }
    }

    private void broadcastToAdmins(ServerContext context) {
        try {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("type", "ADMIN_GET_ONLINE_AUCTIONS_RESPONSE");
            responseMap.put("status", "SUCCESS");
            responseMap.put("auctionList", context.getActiveAuctions());

            String message = safeGson.toJson(responseMap);
            for (WebSocket client : context.getConnectedClients()) {
                if (client.isOpen()) {
                    client.send(message);
                }
            }
        } catch (Exception e) {
            System.err.println("[Broadcast Admin Error] Lỗi phát tin admin: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, String errorMsg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", errorMsg);
        if (conn.isOpen()) {
            conn.send(safeGson.toJson(response));
        }
    }
}