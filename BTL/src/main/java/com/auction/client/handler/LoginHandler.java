package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler; // Đổi sang annotation dùng cho auto-scan
import com.auction.client.controller.LoginController;
import com.auction.client.network.IClientHandler;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.protocol.Response;

import static com.auction.client.utils.NavigationService.navigate;
import com.auction.common.model.user.User;
import com.auction.client.controller.SomeGlobal;

@ResponseHandler(type = "LOGIN_RESPONSE") // Khớp với Type mà Server gửi về
public class LoginHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        // 1. Kiểm tra null an toàn
        if (response == null) return;

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (response.getData() != null) {
                User user = new User();
                user.setEmail((String) response.getData().get("email"));
                user.setUsername((String) response.getData().get("name"));
                user.setId((String) response.getData().get("id"));
                SomeGlobal.setCurrentUser(user);
                // 2. Chuyển màn hình Home (NavigationService đã có Platform.runLater)
                navigate("/com/auction/client/view/home.fxml", "Auction - Trang chủ", true);
                RequestSender.sendGetActiveAuctionsRequest();
                // 3. Hủy đăng ký vì màn hình Login đã đóng, không cần giữ lại trong Registry
                ControllerRegistry.unregister("LoginController");
            } else {
                // 4. Trường hợp thất bại: Tìm LoginController đang hiển thị để báo lỗi
                LoginController controller = ControllerRegistry.get("LoginController");

                if (controller != null) {
                    // Lấy message từ server hoặc dùng câu thông báo mặc định
                    String errorMsg = (response.getMessage() != null && !response.getMessage().isBlank())
                            ? response.getMessage()
                            : "Đăng nhập thất bại không nguyên do. Vui lòng kiểm tra lại!";

                    // Gọi hàm cập nhật UI (Hàm này trong Controller đã có Platform.runLater)
                    controller.updateAnnouncement(errorMsg);
                }
            }
        }
    }
}