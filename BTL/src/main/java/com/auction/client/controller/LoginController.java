package com.auction.client.controller;

import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.MessageListener;
import com.auction.client.network.NetworkClient;
import com.auction.client.network.RequestSender;

import com.auction.client.utils.ValidationUtils;

import com.auction.protocol.Response;
import com.google.gson.Gson;

import javafx.application.Platform;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import javafx.scene.layout.StackPane;

import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML
    private Label announcement;

    @FXML
    private TextField mail;

    @FXML
    private PasswordField passwordtext;

    private final Gson gson = new Gson();

    // =========================================================
    // CHUYỂN SANG MÀN REGISTER
    // =========================================================
    @FXML
    public void handleButtonClick(ActionEvent event)
            throws IOException {

        Parent root = FXMLLoader.load(
                getClass().getResource(
                        "/com/auction/client/view/register.fxml"
                )
        );

        Scene scene = new Scene(root);

        Stage stage = (Stage)
                ((Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setScene(scene);

        stage.show();
    }

    // =========================================================
    // LOGIN
    // =========================================================
    @FXML
    public void handleGetText(ActionEvent event) {

        String email = mail.getText().trim();

        String password = passwordtext.getText();

        // =====================================================
        // VALIDATE: xác thực bước 1
        // =====================================================
        if (email.isEmpty() || password.isEmpty()) {

            announcement.setText(
                    "Vui lòng nhập đầy đủ thông tin!"
            );

            return;
        }

        if (!ValidationUtils
                .isValidCredentials(email, password)) {

            announcement.setText(
                    "Email không hợp lệ hoặc mật khẩu dưới 6 ký tự!"
            );

            return;
        }

        // =====================================================
        // CONNECT SERVER
        // =====================================================

        announcement.setText(
                "Đang kết nối tới Server..."
        );
        NetworkClient.connectAndKeepAlive();
        if (!NetworkClient.isConnected()) {

            announcement.setText(
                    "Không thể kết nối tới server!"
            );

            return;
        }

        // =====================================================
        // LOGIN RESPONSE LISTENER
        // =====================================================
        MessageListener loginListener =
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
                                // LOGIN SUCCESS
                                // =============================
                                if ("SUCCESS".equals(
                                        response.getStatus()
                                )) {

                                    announcement.setText(
                                            "Đăng nhập thành công!"
                                    );

                                    navigateToHome(event);

                                }

                                // =============================
                                // LOGIN FAILED
                                // =============================
                                else {

                                    announcement.setText(
                                            response.getMessage()
                                    );
                                }

                                // remove listener sau khi xử lý
                                ClientMessageDispatcher
                                        .unregister(
                                                "LOGIN_RESPONSE",
                                                this
                                        );

                            } catch (Exception e) {

                                e.printStackTrace();

                                announcement.setText(
                                        "Lỗi xử lý phản hồi từ server!"
                                );
                            }
                        });
                    }
                };

        // =====================================================
        // REGISTER LISTENER
        // =====================================================
        ClientMessageDispatcher.register(
                "LOGIN_RESPONSE",
                loginListener
        );

        // =====================================================
        // SEND REQUEST
        // =====================================================
        announcement.setText(
                "Đang đăng nhập..."
        );

        RequestSender.sendLoginRequest(
                email,
                password
        );
    }

    // =========================================================
    // CHUYỂN SANG HOME
    // =========================================================
    private void navigateToHome(ActionEvent event)
            throws IOException {

        FXMLLoader homeLoader =
                new FXMLLoader(
                        getClass().getResource(
                                "/com/auction/client/view/home.fxml"
                        )
                );

        Parent homeRoot = homeLoader.load();

        HomeController homeController =
                homeLoader.getController();

        SomeGlobal.storeHomeController(
                homeController
        );

        StackPane mainView =
                FXMLLoader.load(
                        getClass().getResource(
                                "/com/auction/client/view/main.fxml"
                        )
                );

        if (homeController != null
                && homeController.getBorderpaneHome()
                != null) {

            homeController
                    .getBorderpaneHome()
                    .setCenter(mainView);
        }

        Stage stage = (Stage)
                ((Node) event.getSource())
                        .getScene()
                        .getWindow();

        stage.setScene(
                new Scene(homeRoot)
        );

        stage.setMaximized(true);

        stage.show();
    }
}