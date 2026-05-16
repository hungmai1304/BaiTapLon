package com.auction.client.network;

import com.auction.client.annotation.ResponseHandler;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonSerializer;
import org.reflections.Reflections;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClientMessageDispatcher {
    // 1. Gia cố Gson: Thêm cả Serializer và Deserializer cho chắc chắn
    public static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .setLenient() // Cho phép parse JSON lỏng lẻo hơn để tránh lỗi vặt
            .create();

    private static final Map<String, IClientHandler> handlerMap = new HashMap<>();

    static {
        init();
    }

    private static void init() {
        try {
            Reflections reflections = new Reflections("com.auction.client.handler");
            Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(ResponseHandler.class);

            for (Class<?> clazz : annotatedClasses) {
                IClientHandler handlerInstance = (IClientHandler) clazz.getDeclaredConstructor().newInstance();
                String type = clazz.getAnnotation(ResponseHandler.class).type();

                // Lưu Key luôn luôn là chữ HOA để tìm kiếm chính xác
                handlerMap.put(type.trim().toUpperCase(), handlerInstance);
                System.out.println("[MessageDispatcher] Registered Handler: " + type);
            }
        } catch (Exception e) {
            System.err.println("[MessageDispatcher] Lỗi khởi tạo Dispatcher: " + e.getMessage());
        }
    }

    public static void dispatch(String message) {
        if (message == null || message.trim().isEmpty()) return;

        try {
            // Parse JSON thành Object Response
            Response response = gson.fromJson(message, Response.class);

            if (response != null && response.getType() != null) {
                String type = response.getType().trim().toUpperCase();
                IClientHandler handler = handlerMap.get(type);

                if (handler != null) {
                    // SỬA TẠI ĐÂY: Truyền đúng tham số mà Interface IClientHandler yêu cầu
                    // Thường là handler.handle(response) hoặc handler.onResponse(response)
                    handler.handle(response);

                    System.out.println("[MessageDispatcher] Đã xử lý: " + type);
                } else {
                    System.err.println("[MessageDispatcher] Chưa đăng ký Handler cho: " + type);
                }
            }
        } catch (Exception e) {
            System.err.println("[MessageDispatcher] Lỗi Parse: " + e.getMessage());
            e.printStackTrace();
        }
    }
}