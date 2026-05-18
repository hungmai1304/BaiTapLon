package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;

import com.auction.client.controller.AdminMainController; // Hoặc controller quản lý chính của Admin
import com.auction.client.controller.LoginController;
import com.auction.client.network.IClientHandler;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.common.model.user.User; // Hoặc model Admin của bạn nếu có riêng
import com.auction.client.controller.SomeGlobal;
import javafx.application.Platform;

import static com.auction.client.utils.NavigationService.navigate;

@ResponseHandler(type=MessageType.ADMIN_LOGIN_RESPONSE)
public class AdminLoginHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        LoginController controller = ControllerRegistry.get("LoginController");
        if (response == null) return;

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (response.getData() != null) {
                // Khởi tạo thông tin Admin dựa trên dữ liệu trả về giống User
                User admin = new User();
                admin.setEmail((String) response.getData().get("email"));
                admin.setUsername((String) response.getData().get("name"));
                admin.setId((String) response.getData().get("id"));

                // Nếu Server có trả về quyền hoặc số dư/thông tin bổ sung của Admin
                if (response.getData().containsKey("balance")) {
                    double balance = ((Number) response.getData().get("balance")).doubleValue();
                    admin.setBalance(balance);
                }

                // Lưu thông tin Admin vào hệ thống toàn cục
                SomeGlobal.setCurrentUser(admin);

                // Thực hiện chuyển màn hình sang Admin Main (Bọc trong Platform.runLater để an toàn cho UI FX)
                Platform.runLater(() -> {
                    navigate("/com/auction/client/view/adminMain.fxml", "Auction - Trang chủ Admin", true);
                });

                // Gửi thêm các request khởi tạo dữ liệu cho hệ thống Admin (ví dụ: lấy danh sách user online)
                RequestSender.send(MessageType.GET_ONLINE_USERS_REQUEST, null);

                // Hủy đăng ký AdminLoginController sau khi đã đăng nhập xong
                ControllerRegistry.unregister("AdminLoginController");
            } else {
                if (controller != null) {
                    Platform.runLater(() -> {
                        String errorMsg = (response.getMessage() != null && !response.getMessage().isBlank())
                                ? response.getMessage()
                                : "Đăng nhập Admin thất bại không nguyên do. Vui lòng kiểm tra lại!";
                        controller.updateAnnouncement(errorMsg);
                    });
                }
            }
        } else {
            if (controller != null) {
                Platform.runLater(() -> {
                    controller.updateAnnouncement(response.getMessage());
                });
            }
        }
    }
}