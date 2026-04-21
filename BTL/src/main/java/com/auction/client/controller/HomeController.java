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

public class HomeController{
    @FXML
    private BorderPane borderpane_home;
    @FXML
    public void handleHomeClicked(ActionEvent event) throws IOException{
        // 1. tải main về
        //2. đặt main vào center
        StackPane main=FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));
        borderpane_home.setCenter(main);

    }
    public BorderPane getBorderpaneHome(){
        return borderpane_home;
    }


    @FXML
    public void handleSearchClicked(ActionEvent event)throws IOException{
        // tai search
        // dat search vao center
        VBox search=FXMLLoader.load(getClass().getResource("/com/auction/client/view/search.fxml"));
        borderpane_home.setCenter(search);
    }
    @FXML
    public void handleTikTokAuction(ActionEvent event) throws IOException{
        //tai tik tok
        // dat tik tok vao center
        VBox tiktok=FXMLLoader.load(getClass().getResource("/com/auction/client/view/tiktokAuction.fxml"));
        borderpane_home.setCenter(tiktok);
    }
    @FXML
    public void handleBankButtonClicked(ActionEvent event) throws IOException{
        AnchorPane bank_view=FXMLLoader.load(getClass().getResource("/com/auction/client/view/bank.fxml"));
        borderpane_home.setCenter(bank_view);
    }





}