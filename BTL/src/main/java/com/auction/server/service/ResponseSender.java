package com.auction.server.service;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class ResponseSender {
    private static final Logger LOGGER = Logger.getLogger(ResponseSender.class.getName());
    // Khởi tạo Gson xử lý chuẩn hóa LocalDateTime giống Client
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    private ResponseSender() {}

    /**
     * Hàm gửi tin nhắn dùng chung cho toàn bộ Server Handler
     * @param conn Kết nối WebSocket của Client nhận tin
     * @param type Loại phản hồi (Ví dụ: "ADMIN_LOGOUT_RESPONSE")
     * @param status Trạng thái phản hồi ("SUCCESS" hoặc "ERROR")
     * @param message Lời nhắn kèm theo từ Server
     * @param data Dữ liệu mở rộng cần gửi kèm (Object, Map, List...), truyền null nếu không có
     */
    public static void send(WebSocket conn, String type, String status, String message, Object data) {
        if (conn == null || !conn.isOpen()) {
            return;
        }

        // Đóng gói tất cả thông tin vào một cấu trúc Map phẳng, dễ bóc tách
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", type);
        responseMap.put("status", status);
        responseMap.put("message", message);

        if (data != null) {
            responseMap.put("data", data);
        }

        // Chuyển thành JSON và đẩy qua WebSocket
        String json = gson.toJson(responseMap);
        conn.send(json);
    }
}