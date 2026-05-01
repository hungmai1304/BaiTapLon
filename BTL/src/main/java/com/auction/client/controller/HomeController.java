package com.auction.client.controller;

import com.auction.client.NetworkClient;
import com.auction.common.model.Product;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import com.google.gson.Gson;
import java.io.IOException;

public class HomeController {
    @FXML
    private BorderPane borderpane_home;

    /**
     * Getter quan trọng của bạn đây, mình đã trả lại nguyên vẹn nhé!
     */
    public BorderPane getBorderpaneHome() {
        return borderpane_home;
    }

    @FXML
    public void handleHomeClicked(ActionEvent event) throws IOException {
        StackPane main = FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));
        borderpane_home.setCenter(main);
    }

    @FXML
    public void handleSearchClicked(ActionEvent event) throws IOException {
        VBox search = FXMLLoader.load(getClass().getResource("/com/auction/client/view/search.fxml"));
        borderpane_home.setCenter(search);
    }

    @FXML
    public void handleTikTokAuction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/tiktokAuction.fxml"));
            VBox tiktokView = loader.load();

            TikTokAuctionController controller = loader.getController();

            // 1. Đăng ký Listener: Khi có tin nhắn từ Server, báo cho Controller này
            NetworkClient.setListener(jsonResponse -> {
                // Nhớ chạy Platform.runLater vì đang ở luồng WebSocket
                javafx.application.Platform.runLater(() -> {
                    Gson gson = new Gson();
                    Product currentProduct = gson.fromJson(jsonResponse, Product.class);
                    controller.setCurrentAuction(currentProduct);
                    System.out.println("Đã cập nhật giá mới trên UI!");
                });
            });

            // 2. Chuyển giao diện ngay lập tức (không bị lag)
            borderpane_home.setCenter(tiktokView);

            // 3. Khởi động kết nối mạng ngầm
            NetworkClient.connectAndKeepAlive();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBankButtonClicked(ActionEvent event) throws IOException {
        StackPane bank_view = FXMLLoader.load(getClass().getResource("/com/auction/client/view/bank.fxml"));
        borderpane_home.setCenter(bank_view);
    }

    @FXML
    public void handleSettingClicked(ActionEvent event) throws IOException {
        AnchorPane setting_view = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Settings.fxml"));
        borderpane_home.setCenter(setting_view);
    }
}