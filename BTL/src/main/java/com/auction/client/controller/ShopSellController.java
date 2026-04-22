package com.auction.client.controller;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ShopSellController {
    @FXML
    public  void handleBackClicked(ActionEvent event) throws IOException {
        VBox shop_view= FXMLLoader.load(getClass().getResource("/com/auction/client/view/Shop.fxml"));
        HomeController homeController=SomeGlobal.getHomeController();
        if(homeController!=null && homeController.getBorderpaneHome()!=null){
            homeController.getBorderpaneHome().setCenter(shop_view);
        }
    }
}