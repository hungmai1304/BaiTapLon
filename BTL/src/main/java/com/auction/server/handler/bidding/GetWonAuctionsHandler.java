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

@CommandMap(MessageType.GET_WON_AUCTIONS_REQUEST)
public class GetWonAuctionsHandler implements IMessageHandler {
    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        // Lấy email từ ServerContext (đã được lưu khi login)
        String email = context.getUserByConn(conn);
        
        if (email == null) {
            Response errResponse = new Response(MessageType.GET_WON_AUCTIONS_RESPONSE, "ERROR", "Vui lòng đăng nhập lại!");
            conn.send(gson.toJson(errResponse));
            return;
        }

        List<Auction> wonAuctions = AuctionDao.getInstance().getWonAuctionsByEmail(email);
        
        Response response = new Response(MessageType.GET_WON_AUCTIONS_RESPONSE, "SUCCESS", "Tải danh sách đấu giá thành công!");
        response.getData().put("wonAuctions", wonAuctions);
        
        conn.send(gson.toJson(response));
    }
}
