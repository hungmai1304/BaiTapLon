package com.auction.client.handler.admin;


import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.Response;
import javafx.application.Platform;

import static com.auction.client.utils.NavigationService.navigate;

@ResponseHandler(type = "ADMIN_FORCE_LOGOUT")
public class AdminForceLogout implements IClientHandler
{

    @Override
    public void handle(Response response) {
        // nhan tin nhan
        Platform.runLater(() -> {
            navigate("/com/auction/client/view/login.fxml", "Login", false);
        });
    }
}
