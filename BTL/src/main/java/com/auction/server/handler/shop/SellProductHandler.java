package com.auction.server.handler.shop;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.AuctionDao;
import com.auction.server.dao.ProductDao;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
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

            //  KIỂM TRA TRẠNG THÁI TRƯỚC KHI BÁN
            Product pCheck = ProductDao.getInstance().getProductById(productId);
            if (pCheck == null) {
                sendError(conn, safeGson, "Sản phẩm không tồn tại trong hệ thống!");
                return;
            }
            if (pCheck.getStatus() == ProductStatus.SOLD) {
                sendError(conn, safeGson, "Thất bại: Sản phẩm này ĐÃ ĐƯỢC CHỐT ĐƠN, không thể đem đấu giá lại!");
                return;
            }

            // 2cho phép cập nhật Database
            boolean isSold = ProductDao.getInstance().sellProduct(productId);

            if (isSold) {
                // Lấy bản mới nhất của Product từ DB lên để đóng gói vào Auction
                Product updatedProduct = ProductDao.getInstance().getProductById(productId);

                if (updatedProduct != null) {
                    // NẾU CHƯA BÁN THÌ BẮT ĐẦU LOGIC TẠO PHIÊN ĐẤU GIÁ
                    String auctionId = String.valueOf(
                            new Random().nextInt(Integer.MAX_VALUE)
                    );

                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime startTime = now.plusMinutes(1);
                    LocalDateTime endTime = startTime.plusMinutes(2);

                    // Đóng gói thành Phiên Đấu Giá (Gắn product trực tiếp vào đây)
                    Auction newAuction = new Auction(
                            updatedProduct,
                            updatedProduct.getStartPrice(),
                            updatedProduct.getStepPrice(),
                            updatedProduct.getStartPrice(),
                            startTime,
                            endTime
                    );
                    newAuction.setStatus("PENDING");
                    newAuction.setId(auctionId);

                    // Cất thẳng vào kho chứa Auction trên RAM (Bỏ qua lưu product độc lập trên RAM)
                    context.addAuction(newAuction);
                }

                // PHẦN HẸN GIỜ TỰ ĐỘNG CHUYỂN TRẠNG THÁI PHIÊN ĐẤU GIÁ
                // =========================================================
                long delayToActive = 1;    // Chờ 30s để bắt đầu (Lúc TEST đổi từ phút thành giây)
                long delayToCompleted = 3; // Tổng 45s để kết thúc

                // Hẹn giờ 1: MỞ BÁT PHIÊN ĐẤU GIÁ (PENDING -> ACTIVE)
                scheduler.schedule(() -> {
                    Auction auctionToStart = context.getAuctionByProductId(productId);
                    if (auctionToStart != null && "PENDING".equals(auctionToStart.getStatus())) {
                        auctionToStart.setStatus("ACTIVE");
                        context.updateAuction(auctionToStart); // Hàm này tự động broadcast update danh sách phiên
                        System.out.println(" [Timer] SP " + productId + " ĐÃ LÊN SÀN ĐẤU GIÁ!");
                    }
                }, delayToActive, TimeUnit.MINUTES);

                // Hẹn giờ 2: KHÓA SỔ PHIÊN ĐẤU GIÁ & HẠ SẢN PHẨM (ACTIVE -> COMPLETED)
                scheduler.schedule(() -> {
                    try {
                        Auction auctionToEnd = context.getAuctionByProductId(productId);
                        if (auctionToEnd != null && !"COMPLETED".equals(auctionToEnd.getStatus())) {

                            // A. Chốt phiên đấu giá thành COMPLETED trên RAM
                            auctionToEnd.setStatus("COMPLETED");
                            context.updateAuction(auctionToEnd);

                            // B. Lấy thông tin sản phẩm gắn liền trong phiên để khôi phục trạng thái dưới DB
                            Product p = auctionToEnd.getProduct();
                            if (p != null) {

                                // XỬ LÝ LƯU THÔNG TIN ĐẤU GIÁ THÀNH CÔNG HOẶC THẤT BẠI
                                if (auctionToEnd.getHighestBidder() != null) {
                                    // TRƯỜNG HỢP 1: CÓ NGƯỜI THẮNG CUỘC
                                    System.out.println("[CHỐT ĐƠN REAL-TIME] Sản phẩm: " + p.getName()
                                            + " | Người thắng: " + auctionToEnd.getHighestBidder().getEmail()
                                            + " | Giá chốt: " + auctionToEnd.getCurrentPrice());

                                    // Đổi trạng thái sản phẩm sang SOLD
                                    p.setStatus(ProductStatus.SOLD);
                                    // Ghi nhận giá chốt đơn cuối cùng vào trường giá hiện tại dưới DB
                                    p.setCurrentPrice(auctionToEnd.getCurrentPrice());
                                    // TRỪ TIỀN :
                                    String winnerEmail = auctionToEnd.getHighestBidder().getEmail();
                                    double finalPrice = auctionToEnd.getCurrentPrice();

                                    UserDao.getInstance().deductBalance(winnerEmail, finalPrice);

                                    System.out.println("Đã trừ " + finalPrice + " từ ví của " + winnerEmail);
                                    String thongBao = " Chúc mừng " + winnerEmail + " đã chốt đơn sản phẩm '" + p.getName() + "' với giá " + String.format("%,.0fđ", finalPrice) + "!";
                                    broadcastAuctionResult(context, safeGson, thongBao);

                                    // LƯU LỊCH SỬ CHỐT ĐƠN VÀO BẢNG AUCTIONS
                                    boolean isSaved = AuctionDao.getInstance().saveCompletedAuction(
                                            auctionToEnd.getId(),
                                            p.getId(),
                                            winnerEmail,
                                            finalPrice
                                    );

                                    if (isSaved) {
                                        System.out.println("Đã ghi nhận lịch sử đấu giá thành công vào Database!");
                                    }

                                } else {
                                    // TRƯỜNG HỢP 2: KHÔNG CÓ AI ĐẶT GIÁ
                                    System.out.println(" [PHIÊN ĐẤU GIÁ THẤT BẠI] Sản phẩm: " + p.getName() + " tự động trả về kho.");
                                    p.setStatus(ProductStatus.AVAILABLE);
                                    String thongBaoE = "Rất tiếc, sản phẩm '" + p.getName() + "' đã hết giờ mà không có ai chốt đơn!";
                                    broadcastAuctionResult(context, safeGson, thongBaoE);
                                    boolean isSaved = AuctionDao.getInstance().saveCompletedAuction(
                                            auctionToEnd.getId(),
                                            p.getId(),
                                            null,
                                            p.getStartPrice()
                                    );
                                    System.out.println("Đã ghi nhận phiên đấu giá không thành công vào Database!");

                                }

                                if (p.getStatus() != ProductStatus.SOLD) {
                                    p.setStartTime(null);
                                    p.setEndTime(null);
                                }

                                // Lưu trực tiếp trạng thái mới nhất (SOLD hoặc AVAILABLE) xuống Database.
                                ProductDao.getInstance().editProduct(p);
                            }


                            //  Xóa phiên đấu giá này ra khỏi danh sách RAM
                            context.removeAuction(auctionToEnd.getId());

                            // D. Thông báo danh sách phiên đấu giá mới nhất cho Client
                            broadcastNewAuctionSession(context, safeGson);

                            System.out.println("[Timer] SP " + productId + " ĐÃ HẾT GIỜ. Phiên kết thúc & Dọn dẹp RAM hoàn tất!");
                        }
                    } catch (Exception e) {
                        System.err.println("[LỖI NGHIÊM TRỌNG] LUỒNG HẸN GIỜ BỊ CRASH: " + e.getMessage());
                        e.printStackTrace();
                    }
                }, delayToCompleted, TimeUnit.MINUTES);

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

    private void broadcastAuctionResult(ServerContext context, Gson gson, String thongBao) {
        Response res = new Response("AUCTION_RESULT_NOTIFICATION", "SUCCESS", thongBao);
        String msg = gson.toJson(res);
        for (WebSocket client : context.getConnectedClients()) {
            if (client.isOpen()) client.send(msg);
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.SELL_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(safeGson.toJson(response));
    }
}