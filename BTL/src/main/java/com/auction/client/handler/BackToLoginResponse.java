package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.Response;
import javafx.application.Platform;
import javafx.scene.control.Alert;

import static com.auction.client.utils.NavigationService.navigate;

@ResponseHandler(type = "BACK_TO_ADMIN_RESPONSE")
public class BackToLoginResponse implements IClientHandler {

    @Override
    public void handle(Response response) {
        // 1. Kiểm tra nếu Server xác thực thành công (SUCCESS)
        if (response != null && "SUCCESS".equalsIgnoreCase(response.getStatus())) {

            // Ép chạy về luồng UI của JavaFX
            Platform.runLater(() -> {
                System.out.println("[BackToLoginResponse] Xác thực thành công. Đang chuyển về màn hình Admin...");

                // Gọi hàm navigate sẵn có của bạn tại đây:
                // Tùy thuộc vào hàm của bạn thuộc class nào (ví dụ: NavigationService.navigate hoặc gọi thẳng nếu kế thừa)
                navigate("/com/auction/client/view/adminMain.fxml", "Auction - Trang chủ Admin", true);
            });

        } else {
            // 2. Nếu thất bại (Bị hack quyền hoặc lỗi hệ thống)
            Platform.runLater(() -> {
                String errorMsg = (response != null) ? response.getMessage() : "Lỗi không xác định!";
                System.err.println("[BackToLoginResponse] Từ chối chuyển view: " + errorMsg);

                // Hiển thị thông báo lỗi trực quan
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Lỗi phân quyền");
                alert.setHeaderText("Thao tác bị từ chối");
                alert.setContentText(errorMsg); // Hiển thị chuỗi: "Bạn đã bị cấm mãi mãi hoặc không có quyền!"
                alert.showAndWait();
            });
        }
    }

    // Nếu hàm `Maps` chưa có sẵn trong phạm vi class này, bạn có thể gọi qua class chứa nó.
    // Ví dụ: NavigationService.navigate("/com/.../adminMain.fxml", "Auction - Trang chủ Admin", true);
}