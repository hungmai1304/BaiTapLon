package com.auction.client;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AuctionClient extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // Nạp file fxml
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/view/login.fxml"));

        Scene scene = new Scene(fxmlLoader.load());
        stage.setX(0);
        stage.setY(0);
        stage.setTitle("JavaFX Button Event");
        stage.setScene(scene);
        stage.setMaximized(true);
        stage.show();

    }

    public static void main(String[] args) {
        launch();
    }
}