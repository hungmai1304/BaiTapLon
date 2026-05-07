package com.auction.client.controller;

import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.MessageListener;
import com.auction.client.network.RequestSender;

import com.auction.protocol.Response;

import com.google.gson.Gson;

import javafx.application.Platform;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;

import javafx.event.ActionEvent;

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

public class RegisterController
        implements Initializable {

    // =========================================================
    // UI
    // =========================================================
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
    private ChoiceBox<String>
            choices_register_openashop;

    @FXML
    private Label resultLabel;

    // =========================================================
    // DATA
    // =========================================================
    private final Gson gson = new Gson();

    private final String[] box = {
            "open a shop",
            "not open a shop"
    };

    // =========================================================
    // BACK TO LOGIN
    // =========================================================
    @FXML
    public void handleBackToLoginButton(
            ActionEvent event
    ) throws IOException {

        Parent root =
                FXMLLoader.load(
                        getClass().getResource(
                                "/com/auction/client/view/login.fxml"
                        )
                );

        Scene scene =
                new Scene(root);

        Stage stage =
                (Stage)
                        ((Node) event.getSource())
                                .getScene()
                                .getWindow();

        stage.setScene(scene);

        stage.show();
    }

    // =========================================================
    // REGISTER
    // =========================================================
    @FXML
    public void handleConfirm(
            ActionEvent event
    ) {

        String registerName =
                boxfield_register_name
                        .getText()
                        .trim();

        String registerEmail =
                textfield_register_email
                        .getText()
                        .trim();

        String password =
                password_register_pass
                        .getText();

        String reconfirm =
                password_register_reconfirm
                        .getText();

        String shopChoice =
                choices_register_openashop
                        .getValue();

        // =====================================================
        // VALIDATE
        // =====================================================
        if (registerName.isEmpty()
                || registerEmail.isEmpty()
                || password.isEmpty()) {

            announcement.setText(
                    "Vui lòng nhập đầy đủ thông tin!"
            );

            return;
        }

        if (!password.equals(reconfirm)) {

            announcement.setText(
                    "Mật khẩu không trùng khớp!"
            );

            return;
        }

        if (!registerEmail.contains("@")) {

            announcement.setText(
                    "Email không hợp lệ!"
            );

            return;
        }

        if (shopChoice == null) {

            announcement.setText(
                    "Vui lòng chọn trạng thái mở shop!"
            );

            return;
        }

        // =====================================================
        // ROLE
        // =====================================================
        String role =
                shopChoice.equals(
                        "open a shop"
                )
                        ? "SELLER"
                        : "BUYER";

        // =====================================================
        // REGISTER LISTENER
        // =====================================================
        MessageListener registerListener =
                new MessageListener() {

                    @Override
                    public void onMessageReceived(
                            String message
                    ) {

                        Platform.runLater(() -> {

                            try {

                                Response response =
                                        gson.fromJson(
                                                message,
                                                Response.class
                                        );

                                // =============================
                                // SUCCESS
                                // =============================
                                if ("SUCCESS".equals(
                                        response.getStatus()
                                )) {

                                    announcement.setStyle(
                                            "-fx-text-fill: green;"
                                    );

                                    announcement.setText(
                                            response.getMessage()
                                    );
                                }

                                // =============================
                                // FAILED
                                // =============================
                                else {

                                    announcement.setStyle(
                                            "-fx-text-fill: red;"
                                    );

                                    announcement.setText(
                                            response.getMessage()
                                    );
                                }

                                // remove listener
                                ClientMessageDispatcher
                                        .unregister(
                                                "REGISTER_RESPONSE",
                                                this
                                        );

                            } catch (Exception e) {

                                e.printStackTrace();

                                announcement.setText(
                                        "Lỗi xử lý phản hồi server!"
                                );
                            }
                        });
                    }
                };

        // =====================================================
        // REGISTER DISPATCHER
        // =====================================================
        ClientMessageDispatcher.register(
                "REGISTER_RESPONSE",
                registerListener
        );

        // =====================================================
        // SEND REQUEST
        // =====================================================
        announcement.setStyle(
                "-fx-text-fill: white;"
        );

        announcement.setText(
                "Đang gửi yêu cầu đăng ký..."
        );

        RequestSender.sendRegisterRequest(
                registerName,
                registerEmail,
                password,
                role
        );
    }

    // =========================================================
    // INIT
    // =========================================================
    @Override
    public void initialize(
            URL location,
            ResourceBundle resources
    ) {

        choices_register_openashop
                .getItems()
                .addAll(box);

        choices_register_openashop
                .setOnAction(this::getChoice);
    }

    // =========================================================
    // CHOICE EVENT
    // =========================================================
    public void getChoice(
            ActionEvent event
    ) {

        String selected =
                choices_register_openashop
                        .getValue();

        resultLabel.setText(
                "Bạn đã chọn: "
                        + selected
        );
    }
}