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

import java.util.Map;

@CommandMap(value = MessageType.REGISTER_BOT_REQUEST)
public class RegisterBotHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("productId");
            double maxPrice = ((Number) data.get("maxPrice")).doubleValue();
            double botStep = ((Number) data.get("botStep")).doubleValue();

            // Lấy email từ session (bảo mật)
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn chưa đăng nhập!");
                return;
            }

            // =========================================================================
            // KIỂM TRA TRẠNG THÁI BLACKLIST TRƯỚC KHI CHO PHÉP ĐĂNG KÝ BOT
            // =========================================================================
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null) {
                sendError(conn, gson, "Lỗi hệ thống: Không tìm thấy thông tin tài khoản của bạn!");
                return;
            }

            // Chặn ngay lập tức nếu user thuộc danh sách đen
            if ("BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                System.err.println("[RegisterBotHandler] Từ chối: Tài khoản BLACKLIST " + userEmail + " cố ý đăng ký Bot tự động!");
                sendError(conn, gson, "Tài khoản của bạn đang nằm trong danh sách đen (BLACKLIST). Không thể sử dụng tính năng Bot đấu giá!");
                return;
            }

            Auction currentAuction = context.getAuctionByProductId(productId);
            if (currentAuction == null || "COMPLETED".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Phiên đấu giá không tồn tại hoặc đã kết thúc!");
                return;
            }

            // KIỂM TRA: Nếu người này đã từng cài Bot cho phiên này rồi thì xóa Bot cũ đi (Cập nhật Bot mới)
            currentAuction.getRegisteredBots().removeIf(bot -> bot.getEmail().equals(userEmail));

            // Đóng gói hợp đồng Bot và nhét vào Phiên đấu giá
            AutoBidConfig newBot = new AutoBidConfig(userEmail, maxPrice, botStep);
            currentAuction.addBot(newBot);

            // Phản hồi về Client
            Response successRes = new Response(MessageType.REGISTER_BOT_RESPONSE, "SUCCESS", "Đã thiết lập Bot thành công!");
            conn.send(gson.toJson(successRes));

            System.out.println("[BOT REGISTER] " + userEmail + " đã cài Bot cho SP: " + productId + " (Max: " + maxPrice + ")");

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi khi đăng ký Bot: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.REGISTER_BOT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}