package com.auction.server.handler.bidding;

import com.auction.common.model.auction.Auction;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.AuctionDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CommandMap(MessageType.GET_WON_AUCTIONS_REQUEST)
public class GetWonAuctionsHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(GetWonAuctionsHandler.class.getName());
    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {

        // Bảo mật an toàn: Lấy trực tiếp từ ServerContext quản lý kết nối
        String email = context.getUserByConn(conn);

        if (email == null) {
            Response errResponse = new Response(MessageType.GET_WON_AUCTIONS_RESPONSE, "ERROR", "Phiên làm việc hết hạn. Vui lòng đăng nhập lại!");
            conn.send(gson.toJson(errResponse));
            return;
        }

        try {
            // Lấy dữ liệu và có khả năng ném ngoại lệ nếu DB chết
            List<Auction> wonAuctions = AuctionDao.getInstance().getWonAuctionsByEmail(email);

            Response response = new Response(MessageType.GET_WON_AUCTIONS_RESPONSE, "SUCCESS", "Tải danh sách đấu giá thành công!");
            response.getData().put("wonAuctions", wonAuctions);
            conn.send(gson.toJson(response));

        } catch (Exception e) {
            // Khi DB lỗi, log lỗi ở Server hệ thống
            LOGGER.severe("[GetWonAuctionsHandler] Lỗi hệ thống khi tải dữ liệu cho: " + email + " | " + e.getMessage());

            // Trả về đúng mã phản hồi lỗi thực tế cho Client biết đường xử lý UI
            Response errResponse = new Response(MessageType.GET_WON_AUCTIONS_RESPONSE, "ERROR", "Lỗi máy chủ: Không thể kết nối đến cơ sở dữ liệu lúc này.");
            conn.send(gson.toJson(errResponse));
        }
    }
}