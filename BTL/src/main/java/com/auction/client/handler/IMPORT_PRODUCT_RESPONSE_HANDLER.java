package com.auction.client.handler;


import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.ShopImportController;
import com.auction.protocol.Response; // Thay đổi import này theo đúng đường dẫn model Response của bạn
import javafx.application.Platform;
@ResponseHandler(type = "IMPORT_PRODUCT_RESPONSE")
public class IMPORT_PRODUCT_RESPONSE_HANDLER {

    public static void handleResponse(Response response) {
        String status = response.getStatus();   // Ví dụ: "SUCCESS" hoặc "ERROR"
        String message = response.getMessage(); // Ví dụ: "Đã lưu sản phẩm thành công!" hoặc "Không thể lưu..."

        boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

        // Lấy instance của Controller đang hoạt động để cập nhật UI
        ShopImportController controller = ShopImportController.getInstance();

        if (controller != null) {
            // Đưa việc cập nhật UI vào JavaFX Application Thread
            Platform.runLater(() -> {
                controller.updateSuccessLabel(message, isSuccess);
            });
        } else {
            System.err.println("[Handler] Không tìm thấy ShopImportController. Thông báo từ server: " + message);
        }
    }
}