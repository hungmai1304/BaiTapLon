package com.auction.client;

import com.auction.client.network.NetworkClient;

import com.auction.client.utils.AdminContext;
import com.auction.client.utils.NavigationService;
import javafx.application.Application;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;

import javafx.scene.Scene;

import javafx.stage.Stage;

import java.util.logging.Logger;

public class AuctionClient extends Application {
    private static final Logger LOGGER = Logger.getLogger(AuctionClient.class.getName());

    @Override
    public void start(Stage stage) throws Exception {
        NavigationService.init(stage);// save stage

        NetworkClient.connectAndKeepAlive();// new thread

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/login.fxml"));

        Scene scene = new Scene(loader.load());

        stage.setScene(scene);

        stage.setTitle("Auction App");
        //-----------------------------------------------------------------------------------
        stage.setOnCloseRequest(event -> {
            LOGGER.info("Đang đóng ứng dụng...");
            AdminContext.getInstance().clear();
            Platform.exit(); // Dừng JavaFX Runtime
            System.exit(0);  // Khai tử hoàn toàn Process (Dùng khi có thread ngầm cứng đầu)
        });
        //-----------------------------------------------------------------------------------
        stage.show();
    }

    public static void main(String[] args) {

        launch(args);
    }
}