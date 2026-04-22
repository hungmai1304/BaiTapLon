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
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;


import java.io.IOException;
import javafx.scene.input.MouseEvent;


public class MainController {
    @FXML
    private GridPane gidpane_main;
    @FXML
    private AnchorPane balance_anchorpane;

    @FXML
    public void handleBalanceClicked(MouseEvent event) throws IOException {
        //tai bank
        //cho bank len giua man hinh
        AnchorPane bank_view = FXMLLoader.load(getClass().getResource("/com/auction/client/view/bank.fxml"));

        HomeController homeController = SomeGlobal.getHomeController();

        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(bank_view);
        }
    }
    @FXML
    public void handleAboutYouClicked(MouseEvent event) throws IOException{
        AnchorPane aboutYou_view = FXMLLoader.load(getClass().getResource("/com/auction/client/view/aboutYou.fxml"));

        HomeController homeController = SomeGlobal.getHomeController();

        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(aboutYou_view);
        }
    }
    @FXML
    public void handleBackMain(ActionEvent event) throws IOException{
        StackPane main_view=FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));


        HomeController homeController = SomeGlobal.getHomeController();

        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(main_view);
        }
    }
    @FXML
    public void handleShopClicked(MouseEvent event) throws IOException{
        VBox shop_view=FXMLLoader.load(getClass().getResource("/com/auction/client/view/Shop.fxml"));
        HomeController homeController=SomeGlobal.getHomeController();
        if(homeController!=null && homeController.getBorderpaneHome()!=null){
            homeController.getBorderpaneHome().setCenter(shop_view);
        }
    }
}




