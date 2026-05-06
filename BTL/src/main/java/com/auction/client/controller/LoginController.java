package com.auction.client.controller;

import javafx.application.Platform; // THÊM THƯ VIỆN NÀY ĐỂ UI KHÔNG BỊ TREO
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import com.auction.client.utils.ValidationUtils;
import com.auction.client.NetworkClient; // THÊM IMPORT NÀY

import java.io.IOException;

// Sửa lại: Nối đường dây kết nối tới server
public class LoginController {
    @FXML
    private Label announcement;
    @FXML
    private TextField mail;
    @FXML
    private PasswordField passwordtext;

    // Nút đăng kí (GIỮ NGUYÊN 100%)
    @FXML
    public void handleButtonClick(ActionEvent event) throws IOException {
        Parent loader = FXMLLoader.load(getClass().getResource("/com/auction/client/view/register.fxml"));
        Scene scene_register = new Scene(loader);
        Stage prStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        prStage.setScene(scene_register);
        prStage.show();
    }

    // Nút đăng nhập: ĐÃ ĐƯỢC "ĐỘ" LẠI ĐỂ GỌI SERVER
    @FXML
    public void handleGetText(ActionEvent event) throws IOException {
        String email = mail.getText().trim();
        String password = passwordtext.getText();

        // 1. Logic cũ của bạn UI: Kiểm tra trống
        if (email.isEmpty() || password.isEmpty()) {
            announcement.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // 2. Logic cũ của bạn UI: Kiểm tra tính hợp lệ
        if (!ValidationUtils.isValidCredentials(email, password)) {
            announcement.setText("Email không hợp lệ hoặc mật khẩu dưới 6 ký tự!");
            return;
        }

        announcement.setText("Đang kết nối đến Server...");

        // 3. CODE MỚI THÊM: Mở đường truyền
        NetworkClient.connectAndKeepAlive();

        // 4. CODE MỚI THÊM: Dán tai nghe chờ Server trả lời
        NetworkClient.setListener(new NetworkClient.MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                // QUAN TRỌNG: JavaFX bắt buộc mọi thay đổi màn hình phải chạy trong Platform.runLater
                Platform.runLater(() -> {
                    System.out.println("[Client] Server trả lời: " + message);

                    // Nếu Server gật đầu (trả về chữ SUCCESS)
                    if (message.contains("\"SUCCESS\"")) {
                        announcement.setText("Xin chào: " + email + " Đang chuyển hướng...");
                        try {
                            // Gọi lại hàm của bạn UI để vô trong
                            navigateToHome(event);
                        } catch (IOException e) {
                            e.printStackTrace();
                            announcement.setText("Lỗi khi tải giao diện chính!");
                        }
                    } else {
                        // Nếu Server lắc đầu
                        announcement.setText("Sai tài khoản hoặc mật khẩu!");
                    }
                });
            }
        });

        // 5. CODE MỚI THÊM: Đóng gói tin nhắn JSON và Gửi đi
        String jsonRequest = "{"
                + "\"type\":\"LOGIN_REQUEST\","
                + "\"data\":{"
                + "\"username\":\"" + email + "\","
                + "\"password\":\"" + password + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(jsonRequest);
    }

    // Hàm chuyển hướng đến màn hình home và main (giữ nguyên)
    private void navigateToHome(ActionEvent event) throws IOException {
        FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/com/auction/client/view/home.fxml"));
        Parent homeRoot = homeLoader.load();

        HomeController homeController = homeLoader.getController();
        SomeGlobal.storeHomeController(homeController);

        StackPane mainView = FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));

        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(mainView);
        }

        Stage prStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        prStage.setScene(new Scene(homeRoot));
        prStage.setMaximized(true);
        prStage.show();
    }
}