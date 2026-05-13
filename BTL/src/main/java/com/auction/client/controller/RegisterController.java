package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.client.utils.ValidationUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.scene.control.Alert;
import javafx.scene.layout.Pane;
import javafx.scene.Node;

import static com.auction.client.utils.NavigationService.navigate;

public class RegisterController implements Initializable {

    @FXML private TextField boxfield_register_name;
    @FXML private TextField textfield_register_email;
    @FXML private PasswordField password_register_pass;
    @FXML private PasswordField password_register_reconfirm;
    @FXML private Label announcement;
    @FXML private ChoiceBox<String> choices_register_openashop;
    @FXML private Label resultLabel;

    private final String[] box = {"open a shop", "not open a shop"};


    @FXML
    private TextField textfield_shop_name;
    @Override
    public void initialize(URL location, ResourceBundle resources) {

        // 1. Ẩn shop name ban đầu
        textfield_shop_name.setVisible(false);
        textfield_shop_name.setManaged(false);

        // 2. Setup ChoiceBox
        choices_register_openashop.getItems().addAll(box);

        // 3. Lắng nghe thay đổi để show/hide shop name
        choices_register_openashop.getSelectionModel().selectedItemProperty().addListener(
                (observable, oldValue, newValue) -> {
                    boolean isSeller = "open a shop".equals(newValue);
                    textfield_shop_name.setVisible(isSeller);
                    textfield_shop_name.setManaged(isSeller);
                    if (!isSeller) {
                        textfield_shop_name.clear();
                    }
                }
        );

        ControllerRegistry.register("RegisterController", this);
    }


    @FXML
    public void handleBackToLoginButton(ActionEvent event) throws IOException {
        ControllerRegistry.unregister("RegisterController");
        navigate("/com/auction/client/view/login.fxml", "Auction - Đăng nhập", false);
    }

    @FXML
    public void handleConfirm(ActionEvent event) {
        String registerName = boxfield_register_name.getText().trim();
        String registerEmail = textfield_register_email.getText().trim();
        String password = password_register_pass.getText();
        String reconfirm = password_register_reconfirm.getText();
        String shopChoice = choices_register_openashop.getValue();

        // Biến để lưu tên shop
        String shopName = null;

        if ("open a shop".equals(shopChoice)) {
            shopName = textfield_shop_name.getText().trim(); // Lấy tên shop ở đây
            if (shopName.isEmpty()) {
                updateAnnouncement("Vui lòng nhập tên shop!", "red");
                return;
            }
        }

        String validationResult = ValidationUtils.validateRegister(
                registerName, registerEmail, password, reconfirm, shopChoice
        );

        if (!"VALID".equals(validationResult)) {
            updateAnnouncement(validationResult, "red");
            return;
        }

        String role = "open a shop".equals(shopChoice) ? "SELLER" : "BIDDER";

        updateAnnouncement("Đang gửi yêu cầu đăng ký...", "white");

        // Cập nhật hàm gọi: Thêm tham số shopName vào cuối
        RequestSender.sendRegisterRequest(registerName, registerEmail, password, role, shopName);
    }

    public void updateAnnouncement(String message, String color) {
        Platform.runLater(() -> {
            announcement.setStyle("-fx-text-fill: " + color + ";");
            announcement.setText(message);
            announcement.setVisible(true);
            announcement.setManaged(true);
        });
    }

    public void getChoice(ActionEvent event) {
        String selected = choices_register_openashop.getValue();
        resultLabel.setText("Bạn đã chọn: " + selected);
    }
}