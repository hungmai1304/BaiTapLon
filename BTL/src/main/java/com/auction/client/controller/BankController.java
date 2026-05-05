package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
