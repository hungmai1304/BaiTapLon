package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

@CommandMap(value = MessageType.UPDATE_AVATAR_REQUEST)
public class UpdateAvatarHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. Kiểm tra đăng nhập
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập!");
                return;
            }

            // 2. Lấy Base64 từ request
            String avatarBase64 = (String) data.get("avatarBase64");
            if (avatarBase64 == null || avatarBase64.isEmpty()) {
                sendError(conn, gson, "Dữ liệu ảnh không hợp lệ!");
                return;
            }

            // 3. Cập nhật vào Database
            boolean isUpdated = UserDao.getInstance().updateUserAvatar(userEmail, avatarBase64);

            if (isUpdated) {
                // Phản hồi thành công kèm theo ảnh để client đồng bộ
                Response response = new Response(MessageType.UPDATE_AVATAR_RESPONSE, "SUCCESS", "Cập nhật ảnh đại diện thành công!");
                response.getData().put("avatarBase64", avatarBase64);
                conn.send(gson.toJson(response));
                System.out.println("[Server] User " + userEmail + " đã cập nhật avatar mới.");
            } else {
                sendError(conn, gson, "Lỗi khi lưu ảnh vào cơ sở dữ liệu.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.UPDATE_AVATAR_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}
