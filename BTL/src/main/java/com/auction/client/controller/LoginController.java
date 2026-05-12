package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class LoginController {

    @FXML private Label announcement;
    @FXML private TextField mail;
    @FXML private PasswordField passwordtext;
    @FXML private CheckBox rememberMe;

    @FXML
    public void initialize() {
        // Khi màn hình được nạp, đăng ký ngay vào Registry
        // Nếu quay lại từ Logout, instance mới sẽ đè lên instance cũ tại đây
        ControllerRegistry.register("LoginController", this);
        // Tự động điền nếu có file ghi nhớ
        loadRememberedUser();
    }
    private void loadRememberedUser() {
        Path path = Paths.get("REMEMBER_ME_FILE.txt");
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path)) {
                String email = reader.readLine();
                String password = reader.readLine();
                if (email != null && password != null) {
                    mail.setText(email);
                    passwordtext.setText(password);
                    rememberMe.setSelected(true);

                    // Tự động đăng nhập nếu muốn:
                    // handleGetText(null);
                }
            } catch (IOException e) {
                System.err.println("Không thể đọc file ghi nhớ.");
            }
        }
    }
    private void deleteRememberFile() {
        try {
            Files.deleteIfExists(Paths.get("REMEMBER_ME_FILE.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleGetText(ActionEvent event) {
        String email = mail.getText().trim();
        String password = passwordtext.getText();

        if (email.isEmpty() || password.isEmpty()) {
            announcement.setText("Vui lòng nhập đầy đủ thông tin!");
            return;
        }

        // Nếu check vào Remember Me, ta sẽ lưu lại sau khi Server phản hồi thành công
        // Nhưng ở đây ta cứ chuẩn bị logic gửi đi trước
        announcement.setText("Đang gửi yêu cầu đăng nhập...");
        RequestSender.sendLoginRequest(email, password);

        // Logic lưu file: Thông thường nên đợi Server trả về SUCCESS mới lưu.
        // Tuy nhiên, nếu bạn muốn lưu ngay tại thời điểm bấm nút:
        if (rememberMe.isSelected()) {
            saveUserCredentials(email, password);
        } else {
            deleteRememberFile();
        }
    }

    private void saveUserCredentials(String email, String password) {
        try {
            Path path = Paths.get("REMEMBER_ME_FILE.txt"); // Lưu ngay tại gốc dự án cho an toàn
            // Hoặc nếu muốn dùng đường dẫn cũ, phải thêm dòng này:
            // Files.createDirectories(path.getParent());

            try (BufferedWriter writer = Files.newBufferedWriter(path)) {
                writer.write(email);
                writer.newLine();
                writer.write(password);
            }
        } catch (IOException e) {
            System.err.println("Lỗi lưu file: " + e.getMessage());
        }
    }

    @FXML
    public void handleButtonClick(ActionEvent event) throws IOException {
        // --- BỔ SUNG Ở ĐÂY ---
        // Trước khi rời sang màn hình Register, hãy hủy đăng ký chính mình
        ControllerRegistry.unregister("LoginController");

        NavigationService.navigate("/com/auction/client/view/register.fxml", "Đăng ký tài khoản", true);
    }

    /**
     * Handler sẽ gọi hàm này. Nếu user gõ sai pass, hàm này chạy,
     * thông báo hiện lên, và user vẫn ở lại màn hình này để sửa pass.
     */
    public void updateAnnouncement(String message) {
        Platform.runLater(() -> announcement.setText(message));
    }
}