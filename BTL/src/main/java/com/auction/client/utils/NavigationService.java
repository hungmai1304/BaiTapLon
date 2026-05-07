package com.auction.client.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.io.IOException;

public class NavigationService {
    private static Stage primaryStage;

    private NavigationService() {}

    /**
     * Cần gọi hàm này một lần duy nhất tại AuctionClient.java (start method)
     */
    public static void init(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Hàm dùng chung để chuyển đổi màn hình
     */
    public static void navigate(String fxmlPath, String title, boolean maximized) {
        // Đảm bảo việc chuyển Scene luôn chạy trên UI Thread
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(NavigationService.class.getResource(fxmlPath));
                Parent root = loader.load();

                Scene scene = new Scene(root);
                primaryStage.setScene(scene);
                primaryStage.setTitle(title);
                primaryStage.setMaximized(maximized);
                primaryStage.show();
            } catch (IOException e) {
                System.err.println("❌ Lỗi khi chuyển màn hình: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }

}