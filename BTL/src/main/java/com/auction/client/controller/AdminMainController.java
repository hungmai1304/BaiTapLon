package com.auction.client.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminMainController implements Initializable {

    @FXML
    private BorderPane mainBorderPane; // Khớp hoàn toàn với fx:id="mainBorderPane"

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SomeGlobal.setAdminMainController(this);

        // Sử dụng runLater để đẩy việc nạp giao diện con xuống cuối hàng đợi Event Queue,
        // Đảm bảo mainBorderPane đã được inject xong hoàn toàn, tránh tuyệt đối lỗi NullPointerException
        Platform.runLater(() -> {
            loadCenterView("/com/auction/client/view/adminOnlineUser.fxml");
        });
    }

    /**
     * Sự kiện khi nhấn vào nút "onlineUsers"
     */
    @FXML
    private void handleOnlineUsersClick(ActionEvent event) {
        loadCenterView("/com/auction/client/view/adminOnlineUser.fxml");
    }

    /**
     * Sự kiện khi nhấn vào nút "onlineAuctions"
     */
    @FXML
    private void handleOnlineAuctionsClick(ActionEvent event) {
        System.out.println("Chuyển sang màn hình quản lý đấu giá trực tuyến");
        // loadCenterView("/com/auction/client/view/adminOnlineAuctions.fxml");
    }

    /**
     * Sự kiện khi nhấn vào nút "AllShop"
     */
    @FXML
    private void handleAllShopClick(ActionEvent event) {
        System.out.println("Chuyển sang màn hình quản lý toàn bộ cửa hàng");
        // loadCenterView("/com/auction/client/view/adminAllShop.fxml");
    }

    /**
     * Sự kiện khi nhấn vào nút "banned list"
     */
    @FXML
    private void handleBannedListClick(ActionEvent event) {
        System.out.println("Chuyển sang màn hình danh sách tài khoản bị cấm");
        // loadCenterView("/com/auction/client/view/adminBannedList.fxml");
    }

    /**
     * Hàm dùng chung để load và đẩy view con vào khu vực Center của BorderPane
     */
    public void loadCenterView(String fxmlPath) {
        if (mainBorderPane == null) {
            System.err.println("[LỖI] mainBorderPane vẫn bị null. Kiểm tra lại file FXML!");
            return;
        }

        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                System.err.println("[LỖI] Không tìm thấy tệp FXML tại đường dẫn: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node node = loader.load();

            // Thay đổi giao diện vùng Center thành công
            mainBorderPane.setCenter(node);
            System.out.println("[AdminMainController] Thay đổi vùng Center thành công: " + fxmlPath);

        } catch (IOException e) {
            System.err.println("[LỖI] Gặp sự cố khi nạp giao diện con: " + fxmlPath);
            e.printStackTrace();
        }
    }
}