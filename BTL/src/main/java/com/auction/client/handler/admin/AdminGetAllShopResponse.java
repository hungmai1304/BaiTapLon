package com.auction.client.handler.admin;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.admin.AllShopController;
import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.logging.Logger;

@ResponseHandler(type="ADMIN_GET_ALL_SHOP_RESPONSE")
public class AdminGetAllShopResponse implements IClientHandler {
private static final Logger LOGGER = Logger.getLogger(AdminGetAllShopResponse.class.getName());
    // Cấu hình Gson chuẩn xử lý Date nếu cấu trúc có mở rộng sau này
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(Response response) {
        if (response == null) return;

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (response.getData() != null && response.getData().containsKey("list")) {

                // 1. Trích xuất mảng dữ liệu cửa hàng nằm trong key "list" (theo chuẩn Server trả về)
                Object shopListData = response.getData().get("list");

                // 2. Ép kiểu chuỗi sang List<AllShopController.ShopItem>
                String jsonStr = gson.toJson(shopListData);
                Type listType = new TypeToken<List<AllShopController.ShopItem>>(){}.getType();
                List<AllShopController.ShopItem> listShops = gson.fromJson(jsonStr, listType);

                // 3. Đưa việc cập nhật UI vào Platform.runLater() tránh lỗi xung đột luồng của JavaFX
                Platform.runLater(() -> {
                    AllShopController controller = SomeGlobal.getAllShopController();
                    if (controller != null) {
                        controller.updateTableData(listShops);
                        LOGGER.info("[AdminGetAllShopResponse] Đã vẽ thành công dữ liệu của " + listShops.size() + " cửa hàng lên giao diện.");
                    } else {
                        LOGGER.severe("[AdminGetAllShopResponse] Lỗi: Chưa khởi tạo hoặc không tìm thấy màn hình AllShopController!");
                    }
                });
            }
        } else {
            LOGGER.severe("[AdminGetAllShopResponse] Server báo lỗi: " + response.getMessage());
        }
    }
}