package com.auction.client.controller.mainHome;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.NavigationService;
import javafx.event.ActionEvent;
import com.auction.common.model.user.User;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import com.auction.client.controller.general.SomeGlobal;

public class HomeController implements Initializable {

    @FXML
    private BorderPane borderpane_home;

    @FXML
    private ToggleButton backToAdmin;

    public BorderPane getBorderpaneHome() {
        return borderpane_home;
    }

    private void loadMainView() throws IOException  {
        StackPane main = FXMLLoader.load(
                getClass().getResource("/com/auction/client/view/main.fxml")
        );
        borderpane_home.setCenter(main);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            SomeGlobal.setHomeController(this);
            loadMainView();
            NavigationService.setTopView("/com/auction/client/view/topView.fxml");

            // Gọi hàm cập nhật hiển thị thanh menu công khai
            refreshMenuVisibility();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Hàm làm mới trạng thái ẩn/hiện của nút Admin (Có thể gọi từ bên ngoài)
     */
    public void refreshMenuVisibility() {
        User currentUser = SomeGlobal.getCurrentUser();
        String role = (currentUser != null && currentUser.getRole() != null) ? currentUser.getRole().trim() : "";

        if ("ADMIN".equalsIgnoreCase(role)) {
            backToAdmin.setVisible(true);
            backToAdmin.setManaged(true);
        } else {
            System.out.println("[Từ chối hành động] Hệ thống ghi nhận bạn không có quyền Admin thực tế.");
            backToAdmin.setVisible(false);
            backToAdmin.setManaged(false);
        }
    }

    @FXML
    public void handleHomeClicked(ActionEvent event) throws IOException {
        loadMainView();
    }

    @FXML
    public void handleSearchClicked(ActionEvent event) throws IOException {
        VBox search = FXMLLoader.load(getClass().getResource("/com/auction/client/view/search.fxml"));
        borderpane_home.setCenter(search);
    }

    @FXML
    public void handleTikTokAuction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/tiktokAuction.fxml"));
            VBox tiktokView = loader.load();
            borderpane_home.setCenter(tiktokView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBankButtonClicked(ActionEvent event) throws IOException {
        StackPane bankView = FXMLLoader.load(getClass().getResource("/com/auction/client/view/bank.fxml"));
        borderpane_home.setCenter(bankView);
    }

    @FXML
    public void handleSettingClicked(ActionEvent event) throws IOException {
        AnchorPane settingView = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Settings.fxml"));
        borderpane_home.setCenter(settingView);
    }

    @FXML
    public void handleBackToAdmin(ActionEvent event) {
        User currentUser = SomeGlobal.getCurrentUser();
        String role = (currentUser != null && currentUser.getRole() != null) ? currentUser.getRole().trim() : "";

        if ("ADMIN".equalsIgnoreCase(role)) {
            System.out.println("[HomeController] Đang gói dữ liệu dạng Object để gửi lên Server...");

            // BỎ DÒNG CŨ: RequestSender.send("BACK_TO_ADMIN_COMMAND", currentUser.getEmail());

            // THAY BẰNG ĐOẠN NÀY: Gói email vào Map để tạo thành cấu trúc { "email": "..." }
            java.util.Map<String, Object> requestPayload = new java.util.HashMap<>();
            requestPayload.put("email", currentUser.getEmail());

            // Gửi map này đi
            RequestSender.send("BACK_TO_ADMIN_COMMAND", requestPayload);
        } else {
            System.out.println("[Từ chối hành động] Hệ thống ghi nhận bạn không có quyền Admin thực tế.");
        }
    }
}