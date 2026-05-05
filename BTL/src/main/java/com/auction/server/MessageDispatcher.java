package com.auction.server;

import com.auction.protocol.Request;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.reflections.Reflections;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class MessageDispatcher {
    private final Map<String, IMessageHandler> handlers = new HashMap<>();
    private final Gson gson;
    private final ServerContext context;

    public MessageDispatcher(Gson gson, ServerContext context) {
        this.gson = gson;
        this.context = context;
        autoRegisterHandlers(); // Gọi hàm tự động quét
    }

    private void autoRegisterHandlers() {
        // Trỏ vào thư mục chứa các Handler của bạn
        Reflections reflections = new Reflections("com.auction.server.handler");

        // Quét tất cả các class có gắn nhãn @CommandMap
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(CommandMap.class);

        for (Class<?> clazz : annotatedClasses) {
            try {
                // Lấy tên lệnh từ nhãn
                String commandName = clazz.getAnnotation(CommandMap.class).value();

                // Khởi tạo thực thể tự động
                IMessageHandler handlerInstance = (IMessageHandler) clazz.getDeclaredConstructor().newInstance();

                // Tự động bỏ vào Map
                handlers.put(commandName.toUpperCase(), handlerInstance);

                System.out.println("✅ Tự động load Handler: " + commandName);
            } catch (Exception e) {
                System.err.println("❌ Lỗi khi khởi tạo Handler: " + clazz.getSimpleName());
            }
        }
    }

    public void dispatch(WebSocket conn, String message) {
        try {
            // 1. SỬ DỤNG CLASS Request CỦA BẠN (Đã import com.auction.protocol.Request)
            Request request = gson.fromJson(message, Request.class);

            // 2. Dùng getType() thay vì getCommand()
            if (request != null && request.getType() != null) {
                IMessageHandler handler = handlers.get(request.getType().toUpperCase());

                if (handler != null) {
                    // 3. Truyền data (Map<String, Object>), gson và context vào cho Handler
                    handler.handle(conn, request.getData(), gson, context);
                } else {
                    conn.send("{\"error\": \"COMMAND_NOT_FOUND\"}");
                }
            }
        } catch (Exception e) {
            conn.send("{\"error\": \"INVALID_JSON\"}");
        }
    }
}