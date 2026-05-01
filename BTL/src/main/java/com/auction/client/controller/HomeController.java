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
        // Chạy ngầm để Render có "ngủ" thì App vẫn không bị treo (tránh lỗi 137)
        new Thread(() -> {
            try {
                System.out.println("Đang triệu hồi Server Render...");
                String jsonResponse = NetworkClient.sendRequest("GET_CURRENT");

                Platform.runLater(() -> {
                    try {
                        if (jsonResponse == null) {
                            System.err.println("Lỗi: Server Render không phản hồi hoặc hết thời gian chờ.");
                            return;
                        }

                        Gson gson = new Gson();
                        Product currentProduct = gson.fromJson(jsonResponse, Product.class);

                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/tiktokAuction.fxml"));
                        VBox tiktokView = loader.load();

                        TikTokAuctionController controller = loader.getController();
                        controller.setCurrentAuction(currentProduct);

                        borderpane_home.setCenter(tiktokView);
                        System.out.println("TikTok Auction đã sẵn sàng!");

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            } catch (Exception e) {
                System.err.println("Lỗi luồng mạng: " + e.getMessage());
            }
        }).start();
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