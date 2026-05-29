package com.auction.server.handler.bidding;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.auction.AutoBidConfig;
import com.auction.common.model.product.Product;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.auction.server.service.AuctionManager; // Import đúng class Manager trung gian
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.Queue;

@CommandMap(value = MessageType.REGISTER_BOT_REQUEST)
public class RegisterBotHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            String productId = (String) data.get("productId");

            if (data.get("maxPrice") == null || data.get("botStep") == null) {
                sendError(conn, gson, "Lỗi: Thiếu cấu hình của Bot!");
                return;
            }

            double maxPrice = ((Number) data.get("maxPrice")).doubleValue();
            double botStep = ((Number) data.get("botStep")).doubleValue();

            if (botStep <= 0) {
                sendError(conn, gson, "Lỗi: Bước giá của Bot phải lớn hơn 0đ!");
                return;
            }

            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Chưa đăng nhập!");
                return;
            }

            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null || "BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                sendError(conn, gson, "Tài khoản không hợp lệ hoặc bị khóa!");
                return;
            }

            if (currentUser.getBalance() < maxPrice) {
                sendError(conn, gson, "Số dư ví không đủ để cài trần Bot!");
                return;
            }

            Auction currentAuction = context.getAuctionByProductId(productId);
            if (currentAuction == null || !"ACTIVE".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Phiên đấu giá không tồn tại hoặc chưa mở sàn!");
                return;
            }

            // Nghiệp vụ chặn chủ sản phẩm cài bot
            if (currentAuction.getProduct() != null && currentAuction.getProduct().getOwner() != null) {
                if (userEmail.equalsIgnoreCase(currentAuction.getProduct().getOwner().getEmail())) {
                    sendError(conn, gson, "Người bán không được phép cài Bot!");
                    return;
                }
            }

            // =========================================================================
            // ĐÃ ĐỒNG BỘ: Lấy hàng đợi từ AuctionManager thay vì gọi hàm không có thật trên Auction
            // =========================================================================
            synchronized (currentAuction) {
                Queue<AutoBidConfig> botQueue = AuctionManager.getInstance().getBotQueue(currentAuction.getId());

                if (botQueue != null) {
                    // Nếu bot cũ của user này đã xếp hàng trước đó, xóa đi để cập nhật cấu hình mới
                    botQueue.removeIf(bot -> bot.getEmail().equals(userEmail));

                    // Khởi tạo Bot mới và ĐẨY XUỐNG CUỐI HÀNG ĐỢI (FIFO) theo cơ chế Round-Robin
                    AutoBidConfig newBot = new AutoBidConfig(userEmail, maxPrice, botStep);
                    botQueue.add(newBot);
                }
            }
            // =========================================================================

            Response successRes = new Response(MessageType.REGISTER_BOT_RESPONSE, "SUCCESS", "Bot đã xếp vào cuối hàng đợi thành công!");
            conn.send(gson.toJson(successRes));

            // Kích hoạt trận chiến đấu giá tự động của Bot
            PlaceBidHandler.triggerBotWar(context, gson, productId, currentAuction);

        } catch (Exception e) {
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.REGISTER_BOT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}