package com.auction.client.controller;

import com.auction.client.network.NetworkClient;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.product.Product;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TikTokAuctionController {

    @FXML private Label name;
    @FXML private Label price;
    @FXML private Label step;

    @FXML
    public void initialize() {
        // Đăng ký controller vào Registry để Handler tìm thấy
        ControllerRegistry.register("TikTokAuctionController", this);

        // Gửi lệnh lấy sản phẩm đầu tiên khi vừa vào màn hình
        NetworkClient.sendCommand("GET_NEXT");
    }

    public void updateUI(Product product) {
        Platform.runLater(() -> {
            if (product != null) {
                name.setText(product.getName());
                price.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
                step.setText(String.format("Bước giá: %,.0f VNĐ", product.getStepPrice()));
            }
        });
    }

    @FXML
    public void handleUp(ActionEvent event) {
        System.out.println("⏳ Đang chuyển sản phẩm trước...");
        NetworkClient.sendCommand("GET_BACK");
    }

    @FXML
    public void handleDown(ActionEvent event) {
        System.out.println("⏳ Đang chuyển sản phẩm tiếp...");
        NetworkClient.sendCommand("GET_NEXT");
    }

    // Gọi khi thoát màn hình đấu giá
    public void cleanup() {
        ControllerRegistry.unregister("TikTokAuctionController");
    }
}