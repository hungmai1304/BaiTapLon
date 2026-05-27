package com.auction.client.handler.admin;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.admin.AdminOnlineAuctions;
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

// Đổi thành "ADMIN_GET_ONLINE_AUCTIONS_RESPONSE" để khớp chính xác gói tin Server trả về
@ResponseHandler(type = "ADMIN_GET_ONLINE_AUCTIONS_RESPONSE")
public class AdminGetOnlineAuctionsResponse implements IClientHandler {

    // Cấu hình Gson chuẩn xử lý Date/Time để đồng bộ dữ liệu RAM từ Server nếu mở rộng
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (com.google.gson.JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(Response response) {
        if (response == null) return;

        // Kiểm tra trạng thái phản hồi từ Server
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (response.getData() != null && response.getData().containsKey("list")) {

                // 1. Trích xuất mảng dữ liệu thô từ cấu trúc dữ liệu "list" bên trong "data"
                Object auctionListData = response.getData().get("list");

                // 2. Chuyển đổi an toàn từ Map/Object sang danh sách DTO của TableView (AdminOnlineAuctions.AuctionItem)
                String jsonStr = gson.toJson(auctionListData);
                Type listType = new TypeToken<List<AdminOnlineAuctions.AuctionItem>>(){}.getType();
                List<AdminOnlineAuctions.AuctionItem> listAuctions = gson.fromJson(jsonStr, listType);

                // 3. Đưa việc cập nhật UI vào Platform.runLater() để tránh lỗi xung đột luồng (FX Application Thread)
                Platform.runLater(() -> {
                    AdminOnlineAuctions controller = SomeGlobal.getAdminOnlineAuctionsController();
                    if (controller != null) {
                        // Đổ dữ liệu vào TableView và làm mới giao diện
                        controller.updateTableData(listAuctions);
                        System.out.println("[AdminGetOnlineAuctionsResponse] Đã tải thành công dữ liệu " + listAuctions.size() + " phiên đấu giá trực tuyến lên bảng.");
                    } else {
                        System.err.println("[AdminGetOnlineAuctionsResponse] Lỗi nghiêm trọng: Chưa khởi tạo hoặc không tìm thấy màn hình AdminOnlineAuctions!");
                    }
                });
            }
        } else {
            System.err.println("[AdminGetOnlineAuctionsResponse] Từ chối từ hệ thống Server: " + response.getMessage());
        }
    }
}