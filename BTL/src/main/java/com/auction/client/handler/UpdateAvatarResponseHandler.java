package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.mainHome.AboutYouController;
import com.auction.client.controller.mainHome.MainController;
import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.controller.mainHome.TopViewController;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;

import java.util.logging.Logger;

@ResponseHandler(type = MessageType.UPDATE_AVATAR_RESPONSE)
public class UpdateAvatarResponseHandler implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(UpdateAvatarResponseHandler.class.getName());
    @Override
    public void handle(Response response) {
        if ("SUCCESS".equals(response.getStatus())) {
            String newBase64 = (String) response.getData().get("avatarBase64");
            LOGGER.info("[Client] " + response.getMessage());

            // Cập nhật vào Global
            User user = SomeGlobal.getCurrentUser();
            if (user != null && newBase64 != null) {
                user.setAvatar(newBase64);
            }

            // Nếu AboutYouController đang mở, yêu cầu nó refresh UI
            AboutYouController controller = ControllerRegistry.get("AboutYouController");
            if (controller != null && newBase64 != null) {
                controller.onUpdateAvatarSuccess(newBase64);
            }

            // Cập nhật cho MainController (Nếu có)
            MainController mainCtrl = SomeGlobal.getMainController();
            if (mainCtrl != null && newBase64 != null) {
                mainCtrl.updateAvatar(newBase64);
            }

            // Cập nhật cho TopViewController (Nếu có)
            TopViewController topCtrl = SomeGlobal.getTopViewController();
            if (topCtrl != null && newBase64 != null) {
                topCtrl.updateAvatar(newBase64);
            }
        } else {
            LOGGER.severe("[Client] Cập nhật avatar thất bại: " + response.getMessage());
        }
    }
}
