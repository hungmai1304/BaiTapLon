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

    // Đổi giá trị mặc định sang kiểu double để đồng bộ tính toán số phút lẻ (ví dụ: 0.5 phút)
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
            // NGHIỆP VỤ ĐỘNG: ĐỔI SANG DOUBLE ĐỂ KHÔNG BỊ MẤT PHẦN LẺ (NHƯ 0.5 PHÚT = 30 GIÂY)
            // =========================================================================
            double waitingMinutes = DEFAULT_TIME_WAITING_TO_START_MINUTES;
            double durationMinutes = DEFAULT_TIME_AUCTION_DURATION_MINUTES;

            try {
                if (data.containsKey("waitingMinutes") && data.get("waitingMinutes") != null) {
                    waitingMinutes = ((Number) data.get("waitingMinutes")).doubleValue();
                }
                if (data.containsKey("durationMinutes") && data.get("durationMinutes") != null) {
                    durationMinutes = ((Number) data.get("durationMinutes")).doubleValue();
                }
            } catch (Exception e) {
                System.err.println("[SellProductHandler] Lỗi parse thời gian động, dùng mặc định: " + e.getMessage());
                waitingMinutes = DEFAULT_TIME_WAITING_TO_START_MINUTES;
                durationMinutes = DEFAULT_TIME_AUCTION_DURATION_MINUTES;
            }

            // Cho phép cập nhật lại giá khởi điểm lúc lên sàn nếu client yêu cầu thay đổi
            double liveStartPrice = pCheck.getStartPrice();
            if (data.containsKey("startPrice") && data.get("startPrice") != null) {
                liveStartPrice = ((Number) data.get("startPrice")).doubleValue();
            }

            LocalDateTime now = LocalDateTime.now();

            // Tính toán thời gian dựa trên số giây quy đổi từ double chính xác tuyệt đối
            long waitingSeconds = Math.round(waitingMinutes * 60);
            long durationSeconds = Math.round(durationMinutes * 60);

            LocalDateTime startTime = now.plusSeconds(waitingSeconds);
            LocalDateTime endTime = startTime.plusSeconds(durationSeconds);

            // GỌI HÀM NGHIỆP VỤ CHUYÊN BIỆT ĐỂ ĐỒNG BỘ XUỐNG DB
            boolean isUpdated = ProductDao.getInstance().sellProduct(productId, liveStartPrice, startTime, endTime);

            if (isUpdated) {
                // Đồng bộ lại dữ liệu Object trong RAM cục bộ
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

                // Đưa phiên đấu giá vào bộ nhớ RAM của Server để quản lý Realtime
                context.addAuction(newAuction);

                // =========================================================================
                // THIẾT LẬP HẸN GIỜ TỰ ĐỘNG (DỰA TRÊN SỐ GIÂY DOUBLE ĐÃ ĐƯỢC TÍNH TOÁN)
                // =========================================================================
                long delayToActiveSeconds = waitingSeconds;
                long delayToCompletedSeconds = waitingSeconds + durationSeconds;

                // HẸN GIỜ 1: MỞ BÁT PHIÊN ĐẤU GIÁ (PENDING -> ACTIVE)
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

                // HẸN GIỜ 2: ĐÓNG PHIÊN VÀ CHỐT ĐƠN (ĐH THỪA HƯỞNG LOGIC PAYOUT BẢN 2)
                scheduler.schedule(() -> {
                    try {
                        Auction auctionToEnd = context.getAuctionByProductId(productId);
                        if (auctionToEnd != null) {

                            // BỌC THÉP CHỐNG LỖI ĐA LUỒNG BẰNG SYNCHRONIZED
                            synchronized (auctionToEnd) {
                                if ("COMPLETED".equals(auctionToEnd.getStatus())) return;

                                // Khóa trạng thái trên RAM ngay lập tức
                                auctionToEnd.setStatus("COMPLETED");
                                context.updateAuction(auctionToEnd);

                                Product p = auctionToEnd.getProduct();
                                if (p != null) {
                                    String winnerName = "Không có";
                                    String thongBao = "";

                                    // TRƯỜNG HỢP 1: CÓ NGƯỜI ĐẶT GIÁ CAO NHẤT
                                    if (auctionToEnd.getHighestBidder() != null) {
                                        String winnerEmail = auctionToEnd.getHighestBidder().getEmail();
                                        winnerName = auctionToEnd.getHighestBidder().getUsername() != null ?
                                                auctionToEnd.getHighestBidder().getUsername() : winnerEmail;
                                        double finalPrice = auctionToEnd.getCurrentPrice();

                                        User winnerUser = UserDao.getInstance().getUserByEmail(winnerEmail);
                                        if (winnerUser != null && winnerUser.getBalance() >= finalPrice) {

                                            // A. TRỪ TIỀN NGƯỜI MUA
                                            UserDao.getInstance().deductBalance(winnerEmail, finalPrice);
                                            p.setStatus(ProductStatus.SOLD);
                                            p.setCurrentPrice(finalPrice);

                                            System.out.println("[CHỐT ĐƠN THÀNH CÔNG] SP: " + p.getName() + " | Winner: " + winnerEmail + " | Giá chốt: " + finalPrice);
                                            thongBao = "Chúc mừng " + winnerName + " đã chốt đơn sản phẩm '" + p.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";

                                            // B. CỘNG TIỀN CHO NGƯỜI BÁN (SELLER PAYOUT)
                                            String sellerEmail = null;
                                            if (p.getOwner() != null) {
                                                if (p.getOwner().getEmail() != null && !p.getOwner().getEmail().trim().isEmpty()) {
                                                    sellerEmail = p.getOwner().getEmail();
                                                } else if (p.getOwner().getId() != null) {
                                                    User sellerFromDB = UserDao.getInstance().findById(p.getOwner().getId());
                                                    if (sellerFromDB != null) sellerEmail = sellerFromDB.getEmail();
                                                }
                                            }

                                            if (sellerEmail != null && !sellerEmail.trim().isEmpty()) {
                                                UserDao.getInstance().depositMoney(sellerEmail, finalPrice);
                                                System.out.println(" [Payout] Đã chuyển " + String.format("%,.0fđ", finalPrice) + " cho Seller: " + sellerEmail);

                                                // Cập nhật lại giao diện số dư Realtime cho Seller nếu đang online
                                                com.auction.server.handler.bidding.PlaceBidHandler.updateClientBalance(context, safeGson, sellerEmail);

                                                // Gửi thông báo riêng (Chúc mừng) tới Seller
                                                String thongBaoRieng = "Sản phẩm '" + p.getName() + "' của bạn đã bán thành công. Bạn nhận được: " + String.format("%,.0fđ", finalPrice);
                                                Response sellerRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBaoRieng);
                                                String sellerMsg = safeGson.toJson(sellerRes);
                                                for (WebSocket client : context.getConnectedClients()) {
                                                    if (sellerEmail.equals(context.getUserByConn(client))) {
                                                        if (client.isOpen()) client.send(sellerMsg);
                                                        break;
                                                    }
                                                }
                                            } else {
                                                System.err.println(" LỖI PAYOUT: Không tìm thấy Email Seller để trả tiền!");
                                            }

                                        } else {
                                            // NGƯỜI THẮNG BÙNG KÈO / HẾT TIỀN TRONG TÀI KHOẢN
                                            p.setStatus(ProductStatus.AVAILABLE);
                                            System.out.println("[CHỐT ĐƠN THẤT BẠI - BÙNG KÈO] Tài khoản " + winnerEmail + " bùng kèo SP " + p.getName());
                                            thongBao = "⚠ Rất tiếc, sản phẩm '" + p.getName() + "' thất bại do người thắng không đủ tiền thanh toán!";
                                            winnerEmail = null;
                                        }

                                        AuctionDao.getInstance().saveCompletedAuction(auctionToEnd.getId(), p.getId(), winnerEmail, finalPrice);

                                    } else {
                                        // TRƯỜNG HỢP 2: HẾT GIỜ MÀ KHÔNG CÓ AI ĐẶT GIÁ
                                        System.out.println(" [PHIÊN KẾT THÚC] Sản phẩm: " + p.getName() + " tự động trả về kho do không có ai đặt giá.");
                                        p.setStatus(ProductStatus.AVAILABLE);
                                        thongBao = "Rất tiếc, sản phẩm '" + p.getName() + "' đã hết giờ đấu giá mà không có ai đặt lệnh!";
                                        AuctionDao.getInstance().saveCompletedAuction(auctionToEnd.getId(), p.getId(), null, p.getStartPrice());
                                    }

                                    if (p.getStatus() != ProductStatus.SOLD) {
                                        p.setStartTime(null);
                                        p.setEndTime(null);
                                    }
                                    ProductDao.getInstance().editProduct(p);

                                    // C. PHÁT LOA CHO TOÀN BỘ HỆ THỐNG ĐỂ HIỂN THỊ POPUP
                                    Response publicRes = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBao);
                                    publicRes.getData().put("auction", auctionToEnd);
                                    publicRes.getData().put("winnerName", winnerName);
                                    String publicMsg = safeGson.toJson(publicRes);
                                    for (WebSocket client : context.getConnectedClients()) {
                                        if (client.isOpen()) client.send(publicMsg);
                                    }
                                }
                            }

                            // Giải phóng RAM và cập nhật danh sách
                            context.removeAuction(auctionToEnd.getId());
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

    private void sendError(WebSocket conn, String msg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(safeGson.toJson(response));
    }
}