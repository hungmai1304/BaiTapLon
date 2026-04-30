package com.auction.client;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class AuctionClient extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        // 1. Load file FXML
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/com/auction/client/view/login.fxml"));
        Parent root = fxmlLoader.load();

        // 2. Tạo Scene dựa trên kích thước gốc của Layout trong FXML
        // Không truyền con số 1200, 700 vào đây để tránh bị "cắt xén" trên màn hình nhỏ
        Scene scene = new Scene(root);

        stage.setTitle("Auction System");
        stage.setScene(scene);

        // 3. Thiết lập hiển thị thông minh
        // Mở to toàn màn hình ngay từ đầu để đảm bảo không bị mất góc
        stage.setMaximized(true);

        // Hoặc nếu bạn vẫn muốn kích thước cố định nhưng an toàn hơn:
        // stage.setWidth(1200);
        // stage.setHeight(700);
        // stage.setResizable(true); // Cho phép bạn bè kéo dãn nếu màn hình họ quá bé

        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}