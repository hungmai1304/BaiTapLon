package com.auction.client;

import com.auction.client.network.NetworkClient;

import javafx.application.Application;

import javafx.fxml.FXMLLoader;

import javafx.scene.Scene;

import javafx.stage.Stage;

public class AuctionClient extends Application {

    @Override
    public void start(Stage stage) throws Exception {

        // CONNECT 1 LẦN DUY NHẤT
        NetworkClient.connectAndKeepAlive();

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
        stage.setMaximized(true);

        stage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}