package com.auction.client.controller.shop;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.controller.mainHome.HomeController;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;


import java.io.IOException;
public class ShopController
{
    @FXML
    public  void handleShopImport(ActionEvent event) throws IOException{
        VBox shopimport_view=FXMLLoader.load(getClass().getResource("/com/auction/client/view/ShopImport.fxml"));
        HomeController homeController= SomeGlobal.getHomeController();
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
