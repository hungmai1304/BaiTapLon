package com.auction.server.handler.tiktok;

import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import java.util.Map;

@CommandMap(value = "STOP_TIK_TOK_LISTENER_REQUEST")
public class StopTiktokListenerHandler implements IMessageHandler {
    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        // Xóa conn ra khỏi Set đăng ký trong RAM
        context.removeTikTokListener(conn);

        // (Tùy chọn) Phản hồi xác nhận cho Client
        Response res = new Response("STOP_TIK_TOK_LISTENER_RESPONSE", "SUCCESS", "Đã hủy đăng ký nhận tin TikTok.");
        conn.send(gson.toJson(res));

        System.out.println("-> [TikTokListener] Một User đã hủy nhận tin (Unsubscribed).");
    }
}