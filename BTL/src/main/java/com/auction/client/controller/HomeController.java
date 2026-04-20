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




}