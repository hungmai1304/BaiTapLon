package com.auction.client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;
import java.io.IOException;

public class AuctionClient extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/view/login.fxml"));

        // Get screen size
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();

        // Create the scene with the screen dimensions
        Scene scene = new Scene(fxmlLoader.load(), screenBounds.getWidth(), screenBounds.getHeight());

        stage.setTitle("Auction");
        stage.setScene(scene);

        // Force the window to be maximized and stay within screen bounds
        stage.setMaximized(true);
        stage.show();


    }

    public static void main(String[] args) {
        launch();
    }
}