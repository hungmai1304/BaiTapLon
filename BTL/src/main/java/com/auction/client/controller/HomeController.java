package com.auction.client.controller;

import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.MessageListener;
import com.auction.client.network.NetworkClient;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.fxml.Initializable;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class HomeController implements Initializable {

    @FXML
    private BorderPane borderpane_home;

    public BorderPane getBorderpaneHome() {

        return borderpane_home;
    }
    //-----------------------------------------------------------------------------------
    private void loadMainView() throws IOException  {
        StackPane main = FXMLLoader.load(
                getClass().getResource("/com/auction/client/view/main.fxml")
        );
        borderpane_home.setCenter(main);
    }
    @Override
    public void initialize(java.net.URL location, java.util.ResourceBundle resources) {
        try {
            SomeGlobal.setHomeController(this);
            // Tự động load main.fxml khi vừa mở Home
            loadMainView();
            String requestParams = "{\"type\":\"GET_PRODUCTS_REQUEST\", \"data\":{}}";
            com.auction.client.network.NetworkClient.sendCommand(requestParams);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //-----------------------------------------------------------------------------------

    // =========================================================
    // HOME
    // =========================================================
    @FXML
    public void handleHomeClicked(ActionEvent event) throws IOException {
        loadMainView();
    }

    // =========================================================
    // SEARCH
    // =========================================================
    @FXML
    public void handleSearchClicked(ActionEvent event)
            throws IOException {

        VBox search =
                FXMLLoader.load(
                        getClass().getResource(
                                "/com/auction/client/view/search.fxml"
                        )
                );

        borderpane_home.setCenter(search);
    }

    // =========================================================
    // TIKTOK AUCTION
    // =========================================================
    @FXML
    public void handleTikTokAuction(ActionEvent event) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource(
                                    "/com/auction/client/view/tiktokAuction.fxml"
                            )
                    );

            VBox tiktokView = loader.load();

            borderpane_home.setCenter(tiktokView);



        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    // =========================================================
    // BANK
    // =========================================================
    @FXML
    public void handleBankButtonClicked(ActionEvent event)
            throws IOException {

        StackPane bankView =
                FXMLLoader.load(
                        getClass().getResource(
                                "/com/auction/client/view/bank.fxml"
                        )
                );

        borderpane_home.setCenter(bankView);
    }

    // =========================================================
    // SETTINGS
    // =========================================================
    @FXML
    public void handleSettingClicked(ActionEvent event)
            throws IOException {

        AnchorPane settingView =
                FXMLLoader.load(
                        getClass().getResource(
                                "/com/auction/client/view/Settings.fxml"
                        )
                );

        borderpane_home.setCenter(settingView);
    }
}