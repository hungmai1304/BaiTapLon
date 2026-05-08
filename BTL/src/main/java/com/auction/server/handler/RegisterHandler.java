package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

@CommandMap(value = MessageType.REGISTER_REQUEST)
public class RegisterHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[RegisterHandler] Đang xử lý yêu cầu Đăng ký...");

        try {
            // 1. Nhặt 4 món đồ từ Client gửi lên (Khớp 100% với giao diện)
            String name = (String) data.get("name");
            String email = (String) data.get("email");
            String password = (String) data.get("password");
            String role = (String) data.get("role"); // Sẽ là "SELLER" hoặc "BUYER"

            // In ra để Terminal để test bằng mắt
            System.out.println("=> Khách mới: " + name + " | Email: " + email + " | Role: " + role);

            // 2. LOGIC ĐĂNG KÝ (Tạm thời cho thành công luôn
            // Mai làm DB xong thì nhét lệnh INSERT INTO vào đây)

            // --- ĐĂNG KÝ THÀNH CÔNG ---
            Response response = new Response(
                    MessageType.REGISTER_RESPONSE,
                    "SUCCESS",
                    "Đăng ký thành công! Chào mừng " + name + " gia nhập hệ thống."
            );

            conn.send(gson.toJson(response));
            System.out.println("[RegisterHandler] Đã báo Đăng ký thành công cho Client.");

        } catch (Exception e) {
            e.printStackTrace();
            Response response = new Response(
                    MessageType.REGISTER_RESPONSE,
                    "ERROR",
                    "Lỗi Server khi đăng ký: " + e.getMessage()
            );
            conn.send(gson.toJson(response));
        }
    }
}