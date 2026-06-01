package com.auction.server.handler.tiktok;

import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.logging.Logger;

@CommandMap(value = "TIK_TOK_LISTENER_REQUEST")
public class TiktokListenerHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(TiktokListenerHandler.class.getName());

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        // 1. Kiểm tra đăng nhập (Lấy email từ connection)
        String userEmail = context.getUserByConn(conn);
        if (userEmail == null) {
            // Hàm sendError mày tự định nghĩa hoặc viết trực tiếp:
            Response err = new Response("TIK_TOK_LISTENER_RESPONSE", "ERROR", "Bạn cần đăng nhập để đăng ký Listener!");
            conn.send(gson.toJson(err));
            return;
        }

        // 2. Thêm vào danh sách đăng ký nhận tin trong RAM
        context.addTikTokListener(conn);

        // 3. Phản hồi cho người dùng biết là đã đăng ký thành công
        Response success = new Response("TIK_TOK_LISTENER_RESPONSE", "SUCCESS", "Đã đăng ký nhận tin TikTok thành công!");
        conn.send(gson.toJson(success));

        LOGGER.info("-> [TikTokListener] User " + userEmail + " đã đăng ký nhận tin.");
    }
}