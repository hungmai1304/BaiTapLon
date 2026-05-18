package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.AdminContext;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@ResponseHandler(type = MessageType.GET_ADMIN_REQUEST_LIST_RESPONSE)
public class AdminRequestHandler implements IClientHandler {

    // Tận dụng Gson để parse danh sách Object từ Server tránh lỗi ép kiểu LinkedTreeMap của Gson
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new TypeAdapter<LocalDateTime>() {
                private final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

                @Override
                public void write(JsonWriter out, LocalDateTime value) throws IOException {
                    if (value == null) {
                        out.nullValue();
                    } else {
                        out.value(value.format(formatter));
                    }
                }

                @Override
                public LocalDateTime read(JsonReader in) throws IOException {
                    if (in.peek() == JsonToken.NULL) {
                        in.nextNull();
                        return null;
                    }
                    return LocalDateTime.parse(in.nextString(), formatter);
                }
            }.nullSafe()) // Đảm bảo an toàn khi gặp giá trị null
            .create();
    @Override
    public void handle(Response response) {
        if (response == null) {
            System.err.println("[AdminRequest] Lỗi: Phản hồi từ server bị trống (null)!");
            return;
        }

        // Kiểm tra trạng thái của gói tin phản hồi từ Server
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (response.getData() != null && response.getData().containsKey("users")) {
                try {
                    // Trích xuất Object chứa danh sách users từ map data
                    Object usersData = response.getData().get("users");

                    // Thực hiện convert JSON an toàn sang List<User>
                    String jsonList = gson.toJson(usersData);
                    Type listType = new TypeToken<List<User>>(){}.getType();
                    List<User> requestList = gson.fromJson(jsonList, listType);

                    // Đẩy dữ liệu mới vào Context phục vụ cập nhật UI tự động thông qua ObservableList
                    AdminContext.getInstance().setAdminRequests(requestList);
                    System.out.println("[AdminRequest] Đã cập nhật thành công " + requestList.size() + " tài khoản chờ duyệt vào hệ thống hiển thị.");

                } catch (Exception e) {
                    System.err.println("[AdminRequest] Lỗi trong quá trình phân tích danh sách người dùng: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("[AdminRequest] Nhận gói tin SUCCESS nhưng không tìm thấy dữ liệu 'users'.");
            }
        } else {
            // Trường hợp SERVER trả về lỗi (Ví dụ: Không có quyền truy cập, tài khoản không phải Admin,...)
            String errorMsg = (response.getMessage() != null && !response.getMessage().isBlank())
                    ? response.getMessage()
                    : "Không xác định được nguyên nhân thất bại từ Server.";

            System.err.println("[AdminRequest] Thao tác thất bại: " + errorMsg);
        }
    }
}