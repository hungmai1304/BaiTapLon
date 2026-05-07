package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.client.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;

public class LoginController {

    @FXML private Label announcement;
    @FXML private TextField mail;
    @FXML private PasswordField passwordtext;

    @FXML
    public void initialize() {
        // Khi màn hình được nạp, đăng ký ngay vào Registry
        // Nếu quay lại từ Logout, instance mới sẽ đè lên instance cũ tại đây
        ControllerRegistry.register("LoginController", this);
    }

    @FXML
    public void handleGetText(ActionEvent event) {
        String email = mail.getText().trim();
        String password = passwordtext.getText();

        if (email.isEmpty() || password.isEmpty()) {
            announcement.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        if (!ValidationUtils.isValidCredentials(email, password)) {
            announcement.setText("Email không hợp lệ hoặc mật khẩu quá ngắn!");
            return;
        }

        announcement.setText("⏳ Đang gửi yêu cầu đăng nhập...");
        RequestSender.sendLoginRequest(email, password);
    }

    @FXML
    public void handleButtonClick(ActionEvent event) throws IOException {
        // --- BỔ SUNG Ở ĐÂY ---
        // Trước khi rời sang màn hình Register, hãy hủy đăng ký chính mình
        ControllerRegistry.unregister("LoginController");

        NavigationService.navigate("/com/auction/client/view/register.fxml", "Đăng ký tài khoản", false);
    }

    /**
     * Handler sẽ gọi hàm này. Nếu user gõ sai pass, hàm này chạy,
     * thông báo hiện lên, và user vẫn ở lại màn hình này để sửa pass.
     */
    public void updateAnnouncement(String message) {
        Platform.runLater(() -> announcement.setText(message));
    }
}