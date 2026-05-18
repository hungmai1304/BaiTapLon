package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.AdminContext;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ResponseHandler(type = MessageType.GET_ONLINE_USERS_RESPONSE)
public class getUserOnlineHandler implements IClientHandler {

    // Tích hợp bộ cấu hình Gson có khả năng xử lý LocalDateTime bằng biểu thức Lambda
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(Response response) {
        if (response == null) return;

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (response.getData() != null && response.getData().containsKey("userList")) {

                // Trích xuất Object hoặc chuỗi Json đại diện cho danh sách User
                Object userListData = response.getData().get("userList");

                // Sử dụng Gson đã được cấu hình Adapter để ép kiểu an toàn
                String jsonStr = gson.toJson(userListData);
                Type listType = new TypeToken<List<User>>(){}.getType();
                List<User> listUsers = gson.fromJson(jsonStr, listType);

                // Đổ dữ liệu vào Context của Admin, UI sẽ tự động làm mới ngay lập tức thông qua Binding
                AdminContext.getInstance().setOnlineUsers(listUsers);
                System.out.println("[getUserOnlineHandler] Đã cập nhật " + listUsers.size() + " users online vào hệ thống.");
            }
        } else {
            System.err.println("[getUserOnlineHandler] Lỗi từ Server: " + response.getMessage());
        }
    }
}