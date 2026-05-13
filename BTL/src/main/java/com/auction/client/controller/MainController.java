package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.user.User;
import javafx.application.Platform;
import javafx.fxml.FXML;
import java.time.LocalTime;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;

public class MainController {

    @FXML
    private GridPane gidpane_main;

    @FXML
    private AnchorPane balance_anchorpane;
    @FXML
    private Label HI_USER_NAME;
    @FXML private Label shop_number_product;
    @FXML private Label about_you_name;


    @FXML
    public void initialize() {
        User user = SomeGlobal.getCurrentUser();
        SomeGlobal.setMainController(this);

        if (user != null && HI_USER_NAME != null) {
            String username = user.getUsername();
            about_you_name.setText(username);
            LocalTime now = LocalTime.now();
            int hour = now.getHour();
            String greeting;

            // Chia khung giờ để chào cho chuẩn bài
            if (hour >= 5 && hour < 12) {
                greeting = "GOOD MORNING, SIR ";
            } else if (hour >= 12 && hour < 18) {
                greeting = "GOOD AFTERNOON, SIR ";
            } else if (hour >= 18 && hour < 22) {
                greeting = "GOOD EVENING, SIR ";
            } else {
                greeting = "GOOD NIGHT, SIR ";
            }

            HI_USER_NAME.setText(greeting + username.toUpperCase());
        }
        RequestSender.sendGetShopProductsRequest(user.getId());
    }


    public void updateProductCount(int count) {
        Platform.runLater(() -> {
            if (shop_number_product != null) {
                shop_number_product.setText(count+"");
            }
        });
    }
    @FXML
    public void handleBalanceClicked(MouseEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/bank.fxml");
    }

    @FXML
    public void handleAboutYouClicked(MouseEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/aboutYou.fxml");
    }

    @FXML
    public void handleBackMain(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/main.fxml");
    }

    @FXML
    public void handleShopClicked(MouseEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/Shop.fxml");
    }
}