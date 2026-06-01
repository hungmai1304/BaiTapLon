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
import com.auction.server.service.AuctionManager;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.Queue;
import java.util.logging.Logger;

@CommandMap(value = MessageType.REGISTER_BOT_REQUEST)
public class RegisterBotHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(RegisterBotHandler.class.getName());

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. KIỂM TRA ĐẦU VÀO NHANH (Bảo vệ tài nguyên hệ thống)
            String productId = (String) data.get("productId");
            if (productId == null || data.get("maxPrice") == null || data.get("botStep") == null) {
                sendError(conn, gson, "Lỗi: Thiếu cấu hình của Bot!");
                return;
            }

            double maxPrice = ((Number) data.get("maxPrice")).doubleValue();
            double botStep = ((Number) data.get("botStep")).doubleValue();

            if (botStep <= 0) {
                sendError(conn, gson, "Lỗi: Bước giá của Bot phải lớn hơn 0đ!");
                return;
            }

            // 2. CHECK TRẠNG THÁI TRÊN RAM TRƯỚC (Bẻ gãy request lỗi/spam trong 0ms không tốn tài nguyên)
            Auction currentAuction = context.getAuctionByProductId(productId);
            if (currentAuction == null || !"ACTIVE".equals(currentAuction.getStatus())) {
                sendError(conn, gson, "Phiên đấu giá không tồn tại hoặc chưa mở sàn!");
                return;
            }

            // Check session kết nối trên RAM
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Lỗi bảo mật: Chưa đăng nhập!");
                return;
            }

            // Nghiệp vụ chủ sản phẩm (Vẫn check trên dữ liệu RAM đã có sẵn từ Auction)
            if (currentAuction.getProduct() != null && currentAuction.getProduct().getOwner() != null) {
                if (userEmail.equalsIgnoreCase(currentAuction.getProduct().getOwner().getEmail())) {
                    sendError(conn, gson, "Người bán không được phép cài Bot!");
                    return;
                }
            }

            // 3. ĐI VÀO DATABASE (Lúc này chắc chắn phiên đấu giá đang chạy, an tâm truy vấn)
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser == null || "BLACKLIST".equalsIgnoreCase(currentUser.getStatus())) {
                sendError(conn, gson, "Tài khoản không hợp lệ hoặc bị khóa!");
                return;
            }

            if (currentUser.getBalance() < maxPrice) {
                sendError(conn, gson, "Số dư ví không đủ để cài trần Bot!");
                return;
            }

            // 4. ĐỒNG BỘ CẬP NHẬT HÀNG ĐỢI BOT (Thu hẹp tối đa phạm vi lock)
            synchronized (currentAuction) {
                Queue<AutoBidConfig> botQueue = AuctionManager.getInstance().getBotQueue(currentAuction.getId());
                if (botQueue != null) {
                    // Xóa cấu hình cũ nếu có (O(N) nhưng được cô lập chỉ chạy khi qua hết màng lọc trên)
                    botQueue.removeIf(bot -> bot.getEmail().equals(userEmail));

                    // Thêm Bot mới vào cuối hàng đợi (Cơ chế FIFO)
                    botQueue.add(new AutoBidConfig(userEmail, maxPrice, botStep));
                }
            }

            // 5. TRẢ PHẢN HỒI REALTIME & KÍCH HOẠT LUỒNG NGẦM PHẢN CÔNG
            Response successRes = new Response(MessageType.REGISTER_BOT_RESPONSE, "SUCCESS", "Bot đã xếp vào cuối hàng đợi thành công!");
            conn.send(gson.toJson(successRes));

            // Hàm này bản thân nó đã chạy bất đồng bộ (botScheduler.submit), đẩy ra ngoài lock hoàn toàn hợp lý
            PlaceBidHandler.triggerBotWar(context, gson, productId, currentAuction);

        } catch (Exception e) {
            LOGGER.severe("Lỗi hệ thống tại RegisterBotHandler: " + e.getMessage());
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.REGISTER_BOT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}