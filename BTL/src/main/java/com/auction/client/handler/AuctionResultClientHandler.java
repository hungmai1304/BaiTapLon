package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.Response;
import javafx.application.Platform;

@ResponseHandler(type = "AUCTION_RESULT_NOTIFICATION")
public class AuctionResultClientHandler implements IClientHandler {
    @Override
    public void handle(Response response) {
        Platform.runLater(() -> {
            // Hiện 1 cái Popup thông báo kết quả
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo kết quả đấu giá");
            alert.setHeaderText("KẾT THÚC PHIÊN ĐẤU GIÁ!");
            alert.setContentText(response.getMessage());
            alert.showAndWait();
        });
    }
}