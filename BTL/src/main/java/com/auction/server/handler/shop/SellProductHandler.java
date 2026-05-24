package com.auction.server.handler.shop;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.auction.Auction;
import com.auction.common.model.user.User;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.ProductDao;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.*;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@CommandMap(value = MessageType.SELL_PRODUCT_REQUEST)
public class SellProductHandler implements IMessageHandler {

    // Tạo luồng ThreadPool an toàn xử lý các tác vụ hẹn giờ đóng/mở sàn
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(10);

    // Giá trị cấu hình mặc định (Fallback phòng trường hợp Client không truyền lên)
    private static final long DEFAULT_TIME_WAITING_TO_START_MINUTES = 1;
    private static final long DEFAULT_TIME_AUCTION_DURATION_MINUTES = 5;

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

            // 1. KIỂM TRA TRẠNG THÁI SẢN PHẨM TRƯỚC KHI CHO PHÉP BÁN
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
            // NGHIỆP VỤ ĐỘNG: Lấy thời gian cấu hình do Client/Seller tự chọn
            // =========================================================================
            long waitingMinutes = DEFAULT_TIME_WAITING_TO_START_MINUTES;
            long durationMinutes = DEFAULT_TIME_AUCTION_DURATION_MINUTES;

            if (data.containsKey("waitingMinutes")) {
                waitingMinutes = ((Double) data.get("waitingMinutes")).longValue();
            }
            if (data.containsKey("durationMinutes")) {
                durationMinutes = ((Double) data.get("durationMinutes")).longValue();
            }

            // Cho phép cập nhật lại giá khởi điểm lúc lên sàn nếu client yêu cầu thay đổi
            double liveStartPrice = pCheck.getStartPrice();
            if (data.containsKey("startPrice")) {
                liveStartPrice = ((Double) data.get("startPrice"));
            }

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime startTime = now.plusMinutes(waitingMinutes);
            LocalDateTime endTime = startTime.plusMinutes(durationMinutes);

            // GỌI HÀM NGHIỆP VỤ CHUYÊN BIỆT (Đã tối ưu ở ProductDao thay vì dùng editProduct bừa bãi)
            boolean isUpdated = ProductDao.getInstance().sellProduct(productId, liveStartPrice, startTime, endTime);

            if (isUpdated) {
                // Đồng bộ lại dữ liệu Object trong RAM cục bộ
                pCheck.setStatus(ProductStatus.ON_AUCTION);
                pCheck.setStartPrice(liveStartPrice);
                pCheck.setCurrentPrice(liveStartPrice);
                pCheck.setStartTime(startTime);
                pCheck.setEndTime(endTime);

                // Dùng UUID chuẩn thay cho Random().nextInt() nhằm tránh trùng lặp ID phiên khi hệ thống lớn
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

                // Đưa phiên đấu giá vào bộ nhớ RAM của Server để quản lý Realtime
                context.addAuction(newAuction);

                // =========================================================================
                // THIẾT LẬP HẸN GIỜ TỰ ĐỘNG (DỰA TRÊN THỜI GIAN ĐỘNG)
                // =========================================================================
                long delayToActiveSeconds = waitingMinutes * 60;
                long delayToCompletedSeconds = (waitingMinutes + durationMinutes) * 60;

                // HẸN GIỜ 1: MỞ BÁT PHIÊN ĐẤU GIÁ (PENDING -> ACTIVE) - Sử dụng Đơn vị GIÂY để tăng độ chính xác
                scheduler.schedule(() -> {
                    try {
                        Auction auctionToStart = context.getAuctionByProductId(productId);
                        if (auctionToStart != null && "PENDING".equals(auctionToStart.getStatus())) {
                            auctionToStart.setStatus("ACTIVE");
                            context.updateAuction(auctionToStart);
                            System.out.println(" [Timer] SP " + productId + " đã CHÍNH THỨC LÊN SÀN ĐẤU GIÁ!");
                            broadcastNewAuctionSession(context); // Phát loa cập nhật lại giao diện client
                        }
                    } catch (Exception e) {
                        System.err.println("[Timer Error] Lỗi khi kích hoạt phiên: " + e.getMessage());
                    }
                }, delayToActiveSeconds, TimeUnit.SECONDS);

                // HẸN GIỜ 2: ĐÓNG PHIÊN VÀ CHỐT ĐƠN (ACTIVE/PENDING -> COMPLETED)
                scheduler.schedule(() -> {
                    try {
                        Auction auctionToEnd = context.getAuctionByProductId(productId);
                        if (auctionToEnd != null && !"COMPLETED".equals(auctionToEnd.getStatus())) {

                            // A. Khóa phiên ngay lập tức trên RAM chặn mọi lượt Bid muộn
                            auctionToEnd.setStatus("COMPLETED");
                            context.updateAuction(auctionToEnd);

                            Product p = auctionToEnd.getProduct();
                            if (p != null) {
                                // TRƯỜNG HỢP 1: CÓ NGƯỜI ĐẶT GIÁ CAO NHẤT
                                if (auctionToEnd.getHighestBidder() != null) {
                                    // TRƯỜNG HỢP 1: CÓ NGƯỜI THẮNG CUỘC
                                    String winnerEmail = auctionToEnd.getHighestBidder().getEmail();
                                    double finalPrice = auctionToEnd.getCurrentPrice();

                                    System.out.println("[CHỐT ĐƠN REAL-TIME] Sản phẩm: " + p.getName()
                                            + " | Người thắng: " + winnerEmail
                                            + " | Giá chốt: " + String.format("%,.0fđ", finalPrice));

                                    // Đổi trạng thái sản phẩm sang SOLD
                                    p.setStatus(ProductStatus.SOLD);
                                    // Ghi nhận giá chốt đơn cuối cùng vào trường giá hiện tại dưới DB
                                    p.setCurrentPrice(finalPrice);

                                    System.out.println("[CHỐT ĐƠN THÀNH CÔNG] SP: " + p.getName() + " | Winner: " + winnerEmail + " | Giá chốt: " + finalPrice);
                                    String thongBao = "🎉 Chúc mừng " + winnerEmail + " đã đấu giá thành công sản phẩm '" + p.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";
                                    broadcastAuctionResult(context, thongBao);

                                    // Lưu log lịch sử đấu giá thành công vào Database
                                    AuctionDao.getInstance().saveCompletedAuction(auctionToEnd.getId(), p.getId(), winnerEmail, finalPrice);

                                } else {
                                    // TRƯỜNG HỢP 2: HẾT GIỜ MÀ KHÔNG CÓ AI ĐẶT GIÁ
                                    System.out.println(" 📢 [PHIÊN KẾT THÚC] Sản phẩm: " + p.getName() + " tự động trả về kho do không có lượt đặt giá.");
                                    p.setStatus(ProductStatus.AVAILABLE);
                                    String thongBaoE = "Rất tiếc, sản phẩm '" + p.getName() + "' đã hết giờ đấu giá mà không có ai đặt lệnh!";
                                    broadcastAuctionResult(context, thongBaoE);

                                    AuctionDao.getInstance().saveCompletedAuction(auctionToEnd.getId(), p.getId(), null, p.getStartPrice());
                                }

                                if (p.getStatus() != ProductStatus.SOLD) {
                                    p.setStartTime(null);
                                    p.setEndTime(null);
                                }

                                // Đồng bộ trạng thái cuối cùng (SOLD hoặc AVAILABLE) xuống DB
                                ProductDao.getInstance().editProduct(p);
                            }

                            // Giải phóng bộ nhớ RAM Server sau khi phiên kết thúc (Tránh tràn RAM / Memory Leak)
                            context.removeAuction(auctionToEnd.getId());

                            // Cập nhật Realtime UI cho toàn bộ Client và Admin đang online
                            broadcastNewAuctionSession(context);
                            broadcastToAdmins(context);

                            System.out.println("[Timer] SP " + productId + " đã HẾT GIỜ. Hủy phiên trên RAM hoàn tất!");
                        }
                    } catch (Exception e) {
                        System.err.println("[LỖI NGHIÊM TRỌNG] LUỒNG HẸN GIỜ ĐÓNG PHIÊN BỊ CRASH: " + e.getMessage());
                    }
                }, delayToCompletedSeconds, TimeUnit.SECONDS);

                // 3. Phản hồi kết quả thành công ngay cho Seller gửi yêu cầu
                Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "SUCCESS", "Đã đưa sản phẩm lên sàn đấu giá thành công!");
                conn.send(safeGson.toJson(response));

                System.out.println("-> [SellProduct] Thành công: ID " + productId);

                // 4. Phát loa thông báo cho toàn hệ thống
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

    // PHÁT LOA ĐỒNG BỘ DANH SÁCH CHO CÁC CLIENT THƯỜNG
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

    // PHÁT LOA ĐỒNG BỘ DANH SÁCH CHO CÁC ADMIN ĐANG ONLINE
    private void broadcastToAdmins(ServerContext context) {
        try {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("type", "ADMIN_GET_ONLINE_AUCTIONS_RESPONSE");
            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Danh sách phiên trực tuyến vừa có sự thay đổi!");

            List<Auction> rawActiveAuctions;
            synchronized (context.getActiveAuctions()) {
                rawActiveAuctions = new ArrayList<>(context.getActiveAuctions());
            }

            List<Map<String, Object>> formattedAuctions = new ArrayList<>();
            for (Auction auction : rawActiveAuctions) {
                if (auction == null) continue;

                Map<String, Object> flatItem = new HashMap<>();
                flatItem.put("id", auction.getId());

                if (auction.getProduct() != null) {
                    flatItem.put("productName", auction.getProduct().getName());
                    flatItem.put("status", auction.getProduct().getStatus());
                    if (auction.getProduct().getOwner() != null) {
                        flatItem.put("ownerEmail", auction.getProduct().getOwner().getEmail());
                    } else {
                        flatItem.put("ownerEmail", "Ẩn danh");
                    }
                } else {
                    flatItem.put("productName", "Sản phẩm không xác định");
                    flatItem.put("status", "UNKNOWN");
                    flatItem.put("ownerEmail", "N/A");
                }
                formattedAuctions.add(flatItem);
            }

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("list", formattedAuctions);
            responseMap.put("data", dataMap);

            String jsonMessage = safeGson.toJson(responseMap);

            for (WebSocket client : context.getConnectedClients()) {
                if (client.isOpen()) {
                    String email = context.getUserByConn(client);
                    if (email != null) {
                        User user = context.getUserCacheByEmail(email);
                        if (user == null) {
                            user = UserDao.getInstance().getUserByEmail(email);
                        }
                        if (user != null && "ADMIN".equalsIgnoreCase(user.getRole())) {
                            client.send(jsonMessage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[Admin Broadcast] Lỗi khi phát dữ liệu cho Admin: " + e.getMessage());
        }
    }

    private void broadcastAuctionResult(ServerContext context, String thongBao) {
        Response res = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBao);
        String msg = safeGson.toJson(res);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) client.send(msg);
        }
    }

    private void sendError(WebSocket conn, String msg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(safeGson.toJson(response));
    }
}