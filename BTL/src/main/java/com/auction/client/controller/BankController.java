package com.auction.client.controller;

import com.auction.server.service.AuctionServer;
import com.auction.server.service.AuctionServer;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

import java.awt.*;
import java.io.IOException;

public class BankController {
    @FXML
    private AnchorPane withdraw_anchor;
    @FXML
    private AnchorPane deposit_anchor;
    @FXML
    public void handleWithdrawClicked(ActionEvent event){
        // hien withdraw len, an deposit di
        withdraw_anchor.setVisible(true);
        withdraw_anchor.setManaged(true);
        deposit_anchor.setVisible(false);
        deposit_anchor.setManaged(false);
    }
    @FXML
    public void handleDepositeClicked(ActionEvent event){
        deposit_anchor.setVisible(true);
        deposit_anchor.setVisible(true);
        withdraw_anchor.setVisible(false);
        withdraw_anchor.setManaged(false);
    }
    @FXML
    public void handleClickedBack(ActionEvent event) throws IOException{
        StackPane main_view=FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));


        HomeController homeController = SomeGlobal.getHomeController();

        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(main_view);
        }
    }
}
