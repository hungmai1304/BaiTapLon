package com.auction.client.network;

import com.auction.client.annotation.ResponseHandler;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import org.reflections.Reflections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClientMessageDispatcher {
    private static final Map<String, IClientHandler> handlerMap = new HashMap<>();
    private static final Gson gson = new Gson();

    // Khối static chạy ngay khi Class được nạp vào bộ nhớ (khởi động chương trình)
    static {
        init();
    }

    private static void init() {
        try {
            // 1. Dùng Reflections quét toàn bộ package chứa các Handler
            Reflections reflections = new Reflections("com.auction.client.handler");

            // 2. Tìm các class có gắn nhãn @ResponseHandler
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(ResponseHandler.class);

            for (Class<?> clazz : annotatedClasses) {
                // 3. Khởi tạo thực thể (instance) của Handler đó
                IClientHandler handlerInstance = (IClientHandler) clazz.getDeclaredConstructor().newInstance();

                // 4. Lấy cái "type" từ nhãn dán ra để làm Key trong Map
                String type = clazz.getAnnotation(ResponseHandler.class).type();

                // 5. Cất vào Map
                handlerMap.put(type.toUpperCase(), handlerInstance);
                System.out.println("🚀 [Client] Đã gom Handler cho loại: " + type);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi khi khởi tạo Dispatcher: " + e.getMessage());
        }
    }

    public static void dispatch(String jsonMessage) {
        try {
            Response response = gson.fromJson(jsonMessage, Response.class);
            String type = response.getType().toUpperCase();

            // So sánh type và gọi đúng Handler trong Map
            IClientHandler handler = handlerMap.get(type);

            if (handler != null) {
                handler.handle(response);
            } else {
                System.out.println("⚠️ Không tìm thấy xử lý cho loại tin nhắn: " + type);
            }
        } catch (Exception e) {
            System.err.println("❌ Lỗi phân tích tin nhắn từ Server");
        }
    }
}