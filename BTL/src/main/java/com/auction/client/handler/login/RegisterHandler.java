package com.auction.client.handler.login;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.login.RegisterController;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.protocol.Response;

import java.util.logging.Logger;

import static com.auction.client.utils.NavigationService.navigate;

@ResponseHandler(type = "REGISTER_RESPONSE")
public class RegisterHandler implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(RegisterHandler.class.getName());

    @Override
    public void handle(Response response) {
        RegisterController controller = ControllerRegistry.get("RegisterController");

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (controller != null) {
                controller.updateAnnouncement(response.getMessage(), "green");
            }
            // Đăng ký thành công thường quay về Login sau vài giây hoặc chuyển ngay
            navigate("/com/auction/client/view/login.fxml", "Auction - Đăng nhập", false);
            ControllerRegistry.unregister("RegisterController");
        } else {
            if (controller != null) {
                String error = response.getMessage() != null ? response.getMessage() : "Đăng ký thất bại!";
                controller.updateAnnouncement(error, "red");
            }
        }
    }
}