package com.auction.server;

import com.auction.protocol.MessageType;
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
        autoRegisterHandlers();
    }

    private void autoRegisterHandlers() {
        Reflections reflections = new Reflections("com.auction.server.handler");
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(CommandMap.class);

        for (Class<?> clazz : annotatedClasses) {
            try {
                String commandName = clazz.getAnnotation(CommandMap.class).value();
                IMessageHandler handlerInstance = (IMessageHandler) clazz.getDeclaredConstructor().newInstance();

                // Key chuẩn hóa: Trim và Uppercase
                handlers.put(commandName.trim().toUpperCase(), handlerInstance);
                System.out.println("[Dispatcher] Registered: " + commandName);
            } catch (Exception e) {
                System.err.println("[Dispatcher] Error loading " + clazz.getSimpleName());
            }
        }
    }

    public void dispatch(WebSocket conn, String message) {
        try {
            Request request = gson.fromJson(message, Request.class);

            if (request != null && request.getType() != null) {
                String type = request.getType().trim().toUpperCase();

                // 1. Kiểm tra xem đây có phải là lệnh ngoại lệ (không cần login) không
                // Sửa lại đoạn kiểm tra lệnh ngoại lệ này trong MessageDispatcher.java
                boolean isAuthRequest = type.equals(MessageType.LOGIN_REQUEST)
                        || type.equals(MessageType.REGISTER_REQUEST);


                // 2. Kiểm tra trạng thái Online của kết nối
                String userId = context.getUserByConn(conn);
                boolean isOnline = (userId != null);

                // LOGIC: Nếu không phải lệnh Auth VÀ cũng không online thì chặn đứng
                if (!isAuthRequest && !isOnline) {
                    System.err.println("[Dispatcher] Unauthorized access attempt from: " + conn.getRemoteSocketAddress());
                    conn.send("{\"type\":\"ERROR\",\"message\":\"UNAUTHORIZED_PLEASE_LOGIN\"}");
                    return;
                }

                // 3. Tìm và gọi Handler
                IMessageHandler handler = handlers.get(type);
                if (handler != null) {
                    handler.handle(conn, request.getData(), gson, context);
                } else {
                    System.err.println("[Dispatcher] No handler for: " + type);
                    conn.send("{\"type\":\"ERROR\",\"message\":\"COMMAND_NOT_FOUND\"}");
                }
            }
        } catch (Exception e) {
            System.err.println("[Dispatcher] JSON Error: " + e.getMessage());
            conn.send("{\"type\":\"ERROR\",\"message\":\"INVALID_JSON\"}");
        }
    }
}