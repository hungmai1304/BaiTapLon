package com.auction.server.handler.bidding;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.auction.AutoBidConfig;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.Map;

@CommandMap(value = MessageType.REGISTER_BOT_REQUEST)
public class RegisterBotHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("productId");

            if (data.get("maxPrice") == null || data.get("botStep") == null) {
                sendError(conn, gson, "Lỗi: Thiếu cấu hình hạn mức giá tối đa hoặc bước giá của Bot!");
                return;
            }

            double maxPrice = ((Number) data.get("maxPrice")).doubleValue();
            double botStep = ((Number) data.get("botStep")).doubleValue();

            // BẢO MẬT: Lấy email từ session kết nối kết nối Websocket
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Bạn chưa đăng nhập hoặc phiên làm việc hết hạn!");
                return;
            }

            // KIỂM TRA TRẠNG THÁI BLACKLIST TRƯỚC KHI CHO PHÉP ĐĂNG KÝ
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null) {
                sendError(conn, gson, "Lỗi hệ thống: Không tìm thấy thông tin tài khoản của bạn!");
                return;
            }

            if ("BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                System.err.println("[RegisterBotHandler] Từ chối tài khoản BLACKLIST " + userEmail + " cố ý cài Bot!");
                sendError(conn, gson, "Tài khoản của bạn đã bị khóa (BLACKLIST). Không thể sử dụng tính năng Bot tự động!");
                return;
            }

            Auction currentAuction = context.getAuctionByProductId(productId);
            if (currentAuction == null || "COMPLETED".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Phiên đấu giá không tồn tại hoặc đã kết thúc!");
                return;
            }

            // =========================================================================
            // THAO TÁC THÊM/SỬA BOT THREAD-SAFE: Khóa đối tượng tránh xung đột với luồng Bidding
            // =========================================================================
            synchronized (currentAuction) {
                if (currentAuction.getRegisteredBots() == null) {
                    currentAuction.setRegisteredBots(new ArrayList<>());
                }

                // Nếu người dùng này đã từng cài Bot cho phiên này, xóa cấu hình Bot cũ đi để ghi đè Bot mới
                currentAuction.getRegisteredBots().removeIf(bot -> bot.getEmail().equals(userEmail));

                // Đóng gói cấu hình Bot mới
                AutoBidConfig newBot = new AutoBidConfig(userEmail, maxPrice, botStep);
                currentAuction.getRegisteredBots().add(newBot);
            }

            // Phản hồi về Client đăng ký thành công trước để tối ưu trải nghiệm UI mượt mà
            Response successRes = new Response(MessageType.REGISTER_BOT_RESPONSE, "SUCCESS", "Đã thiết lập cấu hình Bot tự động thành công!");
            conn.send(gson.toJson(successRes));

            System.out.println("[BOT REGISTER] " + userEmail + " đã cài Bot thành công cho SP: " + productId + " (Trần giá Max: " + maxPrice + ")");

            // =========================================================================
            // KÍCH HOẠT CHIẾN TRƯỜNG BOT: Gọi hàm static xử lý bất đồng bộ qua Thread Pool an toàn
            // =========================================================================
            PlaceBidHandler.triggerBotWar(context, gson, productId, currentAuction);

        } catch (Exception e) {
            System.err.println("[RegisterBotHandler] Lỗi hệ thống: " + e.getMessage());
            sendError(conn, gson, "Lỗi hệ thống khi đăng ký Bot: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.REGISTER_BOT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}