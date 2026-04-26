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
    private VBox withdraw;
    @FXML
    private VBox deposit;
    @FXML
    public void handleWithdrawClicked(ActionEvent event){
        // hien withdraw len, an deposit di
        withdraw.setVisible(true);
        withdraw.setManaged(true);
        deposit.setVisible(false);
        deposit.setManaged(false);
    }
    @FXML
    public void handleDepositeClicked(ActionEvent event){
        deposit.setVisible(true);
        deposit.setVisible(true);
        withdraw.setVisible(false);
        withdraw.setManaged(false);
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
