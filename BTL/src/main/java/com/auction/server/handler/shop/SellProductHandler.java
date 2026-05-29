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

    // Bộ đếm giờ độc lập quản lý kích hoạt và đóng phiên
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // Bộ SafeGson chuẩn hóa cấu trúc xử lý dữ liệu thời gian LocalDateTime theo ISO chuẩn của File 2
    private static final Gson safeGson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // LOG DEBUG: Theo dõi dữ liệu đầu vào thực tế gửi lên từ Client
            System.out.println("[SellProductHandler] RAW DATA từ Client gửi lên: " + data);

            String productId = (String) data.get("id");

            if (productId == null || productId.isEmpty()) {
                sendError(conn, "Không tìm thấy mã sản phẩm để lên sàn!");
                return;
            }

            // KIỂM TRA TRẠNG THÁI SẢN PHẨM TRƯỚC KHI LÊN SÀN (Tiêu chuẩn nghiêm ngặt File 2)
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

            // Đọc cấu hình thời gian và giá tùy biến (Hỗ trợ đa dạng cả CamelCase và Snake_case của File 2)
            Object waitingObj = data.get("waitingMinutes") != null ? data.get("waitingMinutes") : data.get("waiting_minutes");
            Object durationObj = data.get("durationMinutes") != null ? data.get("durationMinutes") : data.get("duration_minutes");
            Object startPriceObj = data.get("startPrice") != null ? data.get("startPrice") : data.get("start_price");

            // Ép kiểu dữ liệu nghiêm ngặt qua hàm bổ trợ
            Double waitingMinutes = parseDoubleStrict(waitingObj);
            Double durationMinutes = parseDoubleStrict(durationObj);
            Double liveStartPrice = parseDoubleStrict(startPriceObj);

            // Phòng hờ nếu người bán không đặt giá khởi điểm riêng biệt, lấy mặc định của sản phẩm
            if (liveStartPrice == null) {
                liveStartPrice = pCheck.getStartPrice();
            }

            // KIỂM TRA LUẬT THỜI GIAN BẮT BUỘC ĐẦU VÀO
            if (waitingMinutes == null) {
                sendError(conn, "Thất bại: Server không nhận được thời gian chờ hợp lệ từ Client (waitingMinutes)!");
                return;
            }
            if (durationMinutes == null || durationMinutes <= 0) {
                sendError(conn, "Thất bại: Server không nhận được thời gian chạy sàn hợp lệ từ Client (durationMinutes phải > 0)!");
                return;
            }

            System.out.println("[SellProductHandler] CHẤP NHẬN THỜI GIAN CLIENT -> Chờ: " + waitingMinutes + " phút | Chạy: " + durationMinutes + " phút");

            LocalDateTime now = LocalDateTime.now();

            // Tính toán thời gian thực tế dựa trên số giây quy đổi từ Client gửi lên
            long waitingSeconds = Math.round(waitingMinutes * 60);
            long durationSeconds = Math.round(durationMinutes * 60);

            LocalDateTime startTime = now.plusSeconds(waitingSeconds);
            LocalDateTime endTime = startTime.plusSeconds(durationSeconds);

            // Cập nhật Database đồng bộ hóa thông tin sản phẩm và khoảng thời gian chạy phiên
            boolean isUpdated = ProductDao.getInstance().sellProduct(productId, liveStartPrice, startTime, endTime);

            if (isUpdated) {
                // Thay đổi trạng thái thực thể sản phẩm trên RAM
                pCheck.setStatus(ProductStatus.ON_AUCTION);
                pCheck.setStartPrice(liveStartPrice);
                pCheck.setCurrentPrice(liveStartPrice);
                pCheck.setStartTime(startTime);
                pCheck.setEndTime(endTime);

                // Khởi tạo UUID định danh duy nhất cho phiên đấu giá (File 2)
                String auctionId = UUID.randomUUID().toString();

                // Đóng gói cấu trúc Phiên Đấu Giá mới vào Context
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

                // HẸN GIỜ 1: KÍCH HOẠT PHIÊN ĐẤU GIÁ (PENDING -> ACTIVE)
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

                // HẸN GIỜ 2: ĐÓNG PHIÊN ĐẤU GIÁ VÀ HẠ SẢN PHẨM CHỐT ĐƠN (ACTIVE -> COMPLETED)
                scheduler.schedule(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Auction auctionToEnd = context.getAuctionByProductId(productId);
                            if (auctionToEnd != null && !"COMPLETED".equals(auctionToEnd.getStatus())) {
                                LocalDateTime now = LocalDateTime.now();
                                if (!now.isBefore(auctionToEnd.getEndTime())) {
                                    // Đã thực sự hết giờ, kết thúc phiên
                                    AuctionManager.getInstance().endAuction(auctionToEnd);
                                    broadcastNewAuctionSession(context);
                                    broadcastToAdmins(context);
                                    System.out.println("[Timer] SP " + productId + " đã được xử lý chốt đơn bởi Timer.");
                                } else {
                                    // Phiên bị gia hạn (Anti-sniping), lên lịch lại chạy theo thời gian còn lại
                                    long extraSeconds = java.time.Duration.between(now, auctionToEnd.getEndTime()).getSeconds();
                                    // Chạy lại chính mình sau extraSeconds (tối thiểu 1s để tránh loop vô tận nếu seconds xấp xỉ 0)
                                    scheduler.schedule(this, extraSeconds > 0 ? extraSeconds : 1, TimeUnit.SECONDS);
                                    System.out.println("[Timer Reschedule] SP " + productId + " bị gia hạn Anti-sniping, dời lịch đóng phiên sau " + extraSeconds + "s.");
                                }
                            }
                        } catch (Exception e) {
                            System.err.println("[Timer Error] Lỗi khi kết thúc phiên hẹn giờ: " + e.getMessage());
                        }
                    }
                }, delayToCompletedSeconds, TimeUnit.SECONDS);

                // Phản hồi gói tin thành công cho Client gửi yêu cầu bán sản phẩm
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá thành công!");
                conn.send(safeGson.toJson(response));

                // Đồng bộ dữ liệu real-time toàn server ngay lập tức
                broadcastNewAuctionSession(context);
                broadcastToAdmins(context);

                System.out.println("-> [SellProduct] Thành công tạo phiên đấu giá cho sản phẩm ID: " + productId);
            } else {
                sendError(conn, "Lỗi khi cập nhật trạng thái lên Database!");
            }

        } catch (Exception e) {
            System.err.println("[SellProductHandler] Lỗi hệ thống: " + e.getMessage());
            sendError(conn, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    /**
     * HÀM PARSE DỮ LIỆU NGHIÊM NGẶT (Tiêu chuẩn gốc File 2)
     */
    private Double parseDoubleStrict(Object value) {
        if (value == null) return null;
        try {
            if (value instanceof Number) {
                return ((Number) value).doubleValue();
            } else if (value instanceof String) {
                return Double.parseDouble(((String) value).trim());
            }
        } catch (Exception e) {
            System.err.println("[Format Error] Không thể parse giá trị: " + value);
        }
        return null;
    }

    /**
     * PHÁT LOA DANH SÁCH AUCTION MỚI NHẤT CHO TOÀN BỘ USER ONLINE
     */
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

    /**
     * PHÁT LOA ĐỒNG BỘ LUỒNG QUẢN LÝ DÀNH CHO ADMIN
     */
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

    /**
     * TÍNH NĂNG BỔ TRỢ TỪ FILE 1: Phát thông báo thông tin chốt đơn/kết quả phiên
     */
    private void broadcastAuctionResult(ServerContext context, Gson gson, String thongBao) {
        Response res = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBao);
        String msg = gson.toJson(res);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) {
                client.send(msg);
            }
        }
    }

    private void sendError(WebSocket conn, String errorMsg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", errorMsg);
        if (conn.isOpen()) {
            conn.send(safeGson.toJson(response));
        }
    }
}