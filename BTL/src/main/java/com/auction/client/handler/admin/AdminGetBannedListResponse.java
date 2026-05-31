package com.auction.client.handler.admin;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.admin.BannedListController;
import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.IClientHandler;
import com.auction.common.model.user.User;
import com.auction.common.utils.LocalDateTimeAdapter;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder; // Thêm import này
import javafx.application.Platform;
import javafx.collections.FXCollections;

import java.time.LocalDateTime; // Thêm import này
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@ResponseHandler(type = "ADMIN_GET_BANNED_LIST_RESPONSE")
public class AdminGetBannedListResponse implements IClientHandler {
private static final Logger LOGGER = Logger.getLogger(AdminGetBannedListResponse.class.getName());
    // THAY ĐỔI TẠI ĐÂY: Khởi tạo Gson có cấu hình nạp Adapter bảo vệ java.time
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, new LocalDateTimeAdapter())
            .create();

    @Override
    public void handle(Response response) {
        if (response == null || !"SUCCESS".equalsIgnoreCase(response.getStatus())) {
            return;
        }

        Map<String, Object> dataMap = response.getData();
        if (dataMap == null || !dataMap.containsKey("list")) return;

        try {
            Object rawList = dataMap.get("list");

            // Lúc này khi chạy hàm chuyển đổi bên dưới, Gson sẽ đi qua bộ Adapter của mình
            // và không bao giờ đụng vào lõi phần cứng của Java nữa!
            String jsonList = gson.toJson(rawList);
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<User>>() {}.getType();
            List<User> bannedUsers = gson.fromJson(jsonList, listType);

            BannedListController controller = SomeGlobal.getBannedListController();
            if (controller != null) {
                Platform.runLater(() -> {
                    controller.getUserTable().setItems(FXCollections.observableArrayList(bannedUsers));
                    controller.getUserTable().refresh();
                });
            }
        } catch (Exception e) {
            LOGGER.severe("[Client] Lỗi bóc dữ liệu bảng ban: " + e.getMessage());
            e.printStackTrace(); // In ra để xem cụ thể nếu còn sót kiểu dữ liệu khác
        }
    }
}