package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import java.util.Map;

@CommandMap(value = MessageType.LOGOUT_REQUEST)
public class LogOutHandler implements IMessageHandler {
    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. Kiểm tra đăng nhập
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn chưa đăng nhập hoặc phiên làm việc hết hạn!");
                return;
            }

            // 2. Xóa khỏi danh sách String Connection cơ bản
            context.removeUser(conn);

            // VIẾT THÊM: Xóa thông tin object User khỏi danh sách online và thông báo tới Admin
            context.removeOnlineUserObject(conn);

            // 3. Phản hồi kết quả đăng xuất thành công về cho chính Client đó
            Response response = new Response(MessageType.OTHER, "SUCCESS", "Đăng xuất thành công!");
            conn.send(gson.toJson(response));

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống khi đăng xuất: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.OTHER, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}