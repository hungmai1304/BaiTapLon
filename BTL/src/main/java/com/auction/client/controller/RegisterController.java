package com.auction.client.controller;

import com.auction.client.NetworkClient;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class RegisterController implements Initializable {

    // Nút quay lại đăng nhập
    @FXML
    public void handleBackToLoginButton(ActionEvent event) throws  IOException{
        Parent loader= FXMLLoader.load(getClass().getResource("/com/auction/client/view/login.fxml"));
        Scene scene_login=new Scene(loader);

        Stage prStage=(Stage) ((Node) event.getSource()).getScene().getWindow();
        prStage.setScene(scene_login);
        prStage.show();
    }

    //---------------------------------------------------------------------------------------------
    @FXML
    private TextField boxfield_register_name;
    @FXML
    private TextField textfield_register_email;
    @FXML
    private PasswordField password_register_pass;
    @FXML
    private PasswordField password_register_reconfirm;
    @FXML
    private Label announcement;
    @FXML
    private ChoiceBox<String> choices_register_openashop;
    @FXML
    private Label resultLabel;

    private String[] box = {"open a shop", "not open a shop"};

    // NÚT XÁC NHẬN ĐĂNG KÝ (ĐÃ ĐƯỢC "ĐỘ" LẠI KẾT NỐI SERVER)
    @FXML
    public void handleConfirm(ActionEvent event) throws IOException {
        String registerName = boxfield_register_name.getText().trim();
        String registerEmail = textfield_register_email.getText().trim();
        String password = password_register_pass.getText();
        String reconfirm = password_register_reconfirm.getText();
        String shopChoice = choices_register_openashop.getValue();

        // 1. Giữ nguyên logic kiểm tra lỗi của UI cũ
        if (registerName.isEmpty() || registerEmail.isEmpty() || password.isEmpty()) {
            announcement.setText("Vui lòng nhập đầy đủ thông tin!");
            return; // Bắt buộc phải có return để nó dừng lại, không chạy xuống dưới
        }
        else if (!password.equals(reconfirm)){
            announcement.setText("Mật khẩu không trùng khớp!");
            return;
        }
        else if(!registerEmail.contains("@")){
            announcement.setText("Email Không Hợp Lệ!");
            return;
        }
        else if (shopChoice == null) {
            announcement.setText("Vui lòng chọn trạng thái mở Shop!");
            return;
        }

        // 2. Chuyển đổi lựa chọn Shop thành Role để gửi Server
        String role = shopChoice.equals("open a shop") ? "SELLER" : "BUYER";

        // 3. TIÊM LOGIC MẠNG VÀO ĐÂY
        announcement.setText("Đang gửi yêu cầu đăng ký lên Server...");

        NetworkClient.connectAndKeepAlive();
        NetworkClient.setListener(new NetworkClient.MessageListener() {
            @Override
            public void onMessageReceived(String message) {
                // Đưa UI về luồng chính để không bị treo App
                Platform.runLater(() -> {
                    System.out.println("[Client] Server trả lời: " + message);

                    if (message.contains("\"SUCCESS\"")) {
                        // Thành công -> Báo xanh
                        announcement.setStyle("-fx-text-fill: green;");
                        announcement.setText("Đăng kí thành công! Mời quay lại trang đăng nhập.");
                    } else {
                        // Thất bại -> Báo đỏ
                        announcement.setStyle("-fx-text-fill: red;");
                        announcement.setText("Lỗi: Tài khoản đã tồn tại hoặc lỗi Server!");
                    }
                });
            }
        });

        // 4. Đóng gói 4 thông tin thành chuỗi JSON
        String jsonRequest = "{"
                + "\"type\":\"REGISTER_REQUEST\","
                + "\"data\":{"
                + "\"name\":\"" + registerName + "\","
                + "\"email\":\"" + registerEmail + "\","
                + "\"password\":\"" + password + "\","
                + "\"role\":\"" + role + "\""
                + "}"
                + "}";

        // 5. Gửi lên Server
        NetworkClient.sendCommand(jsonRequest);
    }

    // Khởi tạo ChoiceBox
    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        choices_register_openashop.getItems().addAll(box);
        choices_register_openashop.setOnAction(this::getChoice);
    }

    // Phương thức xử lý khi người dùng chọn một mục
    public void getChoice(javafx.event.ActionEvent event) {
        String selected = choices_register_openashop.getValue();
        resultLabel.setText("Bạn đã chọn: " + selected);
    }
}