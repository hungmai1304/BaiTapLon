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

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        choices_register_openashop.getItems().addAll(box);
        choices_register_openashop.setOnAction(this::getChoice);
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

        String validationResult = ValidationUtils.validateRegister(
                registerName, registerEmail, password, reconfirm, shopChoice
        );

        if (!"VALID".equals(validationResult)) {
            updateAnnouncement(validationResult, "red");
            return;
        }

        String role = shopChoice.equals("open a shop") ? "SELLER" : "BIDDER";

        updateAnnouncement("⏳ Đang gửi yêu cầu đăng ký...", "white");
        RequestSender.sendRegisterRequest(registerName, registerEmail, password, role);
    }

    public void updateAnnouncement(String message, String color) {
        Platform.runLater(() -> {
            announcement.setStyle("-fx-text-fill: " + color + ";");
            announcement.setText(message);
        });
    }

    public void getChoice(ActionEvent event) {
        String selected = choices_register_openashop.getValue();
        resultLabel.setText("Bạn đã chọn: " + selected);
    }
}