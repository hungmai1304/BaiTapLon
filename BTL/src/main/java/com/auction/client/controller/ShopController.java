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


import java.io.IOException;
public class ShopController
{
    @FXML
    public  void handleShopImport(ActionEvent event) throws IOException{
        VBox shopimport_view=FXMLLoader.load(getClass().getResource("/com/auction/client/view/ShopImport.fxml"));
        HomeController homeController=SomeGlobal.getHomeController();
        if(homeController!=null && homeController.getBorderpaneHome()!=null){
            homeController.getBorderpaneHome().setCenter(shopimport_view);
        }
    }
    @FXML
    public  void handleShopSell(ActionEvent event) throws IOException{
        VBox shopsell_view=FXMLLoader.load(getClass().getResource("/com/auction/client/view/ShopSell.fxml"));
        HomeController homeController=SomeGlobal.getHomeController();
        if(homeController!=null && homeController.getBorderpaneHome()!=null){
            homeController.getBorderpaneHome().setCenter(shopsell_view);
        }
    }
    @FXML
    public  void handleBackClicked(ActionEvent event) throws IOException {
        StackPane main_view= FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));
        HomeController homeController=SomeGlobal.getHomeController();
        if(homeController!=null && homeController.getBorderpaneHome()!=null){
            homeController.getBorderpaneHome().setCenter(main_view);
        }
    }


}
