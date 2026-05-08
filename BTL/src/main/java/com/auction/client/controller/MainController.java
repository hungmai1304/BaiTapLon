package com.auction.client.controller;

import com.auction.client.utils.NavigationService;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;

public class MainController {

    @FXML
    private GridPane gidpane_main;

    @FXML
    private AnchorPane balance_anchorpane;

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