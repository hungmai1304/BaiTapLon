package com.auction.client.utils;

import com.auction.client.controller.mainHome.HomeController;
import com.auction.client.controller.general.SomeGlobal;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.io.IOException;
import java.util.logging.Logger;

public class NavigationService {
    private static final Logger LOGGER = Logger.getLogger(NavigationService.class.getName());
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
                primaryStage.setResizable(true);
                primaryStage.show();
            } catch (IOException e) {
                LOGGER.severe("[Navigation] Lỗi khi chuyển màn hình: " + fxmlPath);
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
                    LOGGER.severe("[Navigation] Lỗi: Không tìm thấy HomeController hoặc BorderPane!");
                }
            } catch (IOException e) {
                LOGGER.severe("[Navigation] Lỗi load FXML: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }
    public static void setTopView(String fxmlPath) {
        Platform.runLater(() -> {
            try {
                Parent view = FXMLLoader.load(NavigationService.class.getResource(fxmlPath));
                HomeController homeController = SomeGlobal.getHomeController();

                if (homeController != null && homeController.getBorderpaneHome() != null) {
                    // Thay đổi view ở vùng TOP của BorderPane
                    homeController.getBorderpaneHome().setTop(view);
                } else {
                    LOGGER.severe("Lỗi: Không tìm thấy HomeController hoặc BorderPane khi setTop!");
                }
            } catch (IOException e) {
                LOGGER.severe("Lỗi load FXML ở vùng Top: " + fxmlPath);
                e.printStackTrace();
            }
        });
    }


}