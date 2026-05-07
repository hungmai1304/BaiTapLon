package com.auction.client;

import com.auction.client.network.NetworkClient;

import com.auction.client.utils.NavigationService;
import javafx.application.Application;

import javafx.fxml.FXMLLoader;

import javafx.scene.Scene;

import javafx.stage.Stage;

public class AuctionClient extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        NavigationService.init(stage);// save stage

        NetworkClient.connectAndKeepAlive();// new thread

        FXMLLoader loader =
                new FXMLLoader(
                        getClass().getResource(
                                "/com/auction/client/view/login.fxml"
                        )
                );

        Scene scene =
                new Scene(loader.load());

        stage.setScene(scene);

        stage.setTitle("Auction App");


        stage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}