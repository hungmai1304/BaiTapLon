package com.auction.client.utils;

import com.auction.client.controller.HomeController;
import com.auction.client.controller.SomeGlobal;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
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
                primaryStage.setResizable(false);
                primaryStage.show();
            } catch (IOException e) {
                System.err.println("❌ Lỗi khi chuyển màn hình: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }
    public static void setCenterView(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                Parent view = FXMLLoader.load(NavigationService.class.getResource(fxmlPath));
                HomeController homeController = SomeGlobal.getHomeController();

                if (homeController != null && homeController.getBorderpaneHome() != null) {
                    homeController.getBorderpaneHome().setCenter(view);
                } else {
                    System.err.println("Lỗi: Không tìm thấy HomeController hoặc BorderPane!");
                }
            } catch (IOException e) {
                System.err.println("Lỗi load FXML: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }

}