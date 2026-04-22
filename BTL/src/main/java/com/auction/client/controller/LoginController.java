package com.auction.client.controller;

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


import java.io.IOException;

public class LoginController {
    @FXML
    private Label announcement;
    @FXML
    private TextField mail;
    @FXML
    private PasswordField passwordtext;

    @FXML
    public void handleButtonClick(ActionEvent event) throws  IOException{
        //load giao diện mới
        Parent loader= FXMLLoader.load(getClass().getResource("/com/auction/client/view/register.fxml"));
        Scene scene_register=new Scene(loader);
        //Lấy cửa số gốc
        Stage prStage=(Stage) ((Node) event.getSource()).getScene().getWindow();
        //đặt scene mới lên cửa sổ gốc
        prStage.setScene(scene_register);
        prStage.show();


    }

    @FXML
    public void handleGetText(ActionEvent event) throws IOException {
        String email = mail.getText();
        String password = passwordtext.getText();

        if (email.isEmpty() || password.isEmpty()) {
            announcement.setText("Vui lòng nhập đầy đủ thông tin");
        } else {
            boolean isValid;
            if(email.contains("@") && password.length()>=6){
                isValid=true;
            }
            else{
                isValid=false;
            }
            if (isValid) {
                announcement.setText("Xin chào: " + email + " đang chuyển hướng");

                // 1. CHỈ DÙNG 1 LOADER DUY NHẤT CHO HOME
                FXMLLoader home_loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/home.fxml"));
                Parent homeRoot = home_loader.load(); // PHẢI GỌI .load() TRƯỚC KHI LẤY CONTROLLER

                // 2. LẤY CONTROLLER SAU KHI ĐÃ LOAD
                HomeController homeController = home_loader.getController();
                SomeGlobal.storeHomeController(homeController);

                // 3. LOAD MAIN FXML (VÙNG CENTER)
                StackPane mainView = FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));

                // 4. ĐƯA MAIN VÀO CENTER CỦA HOME (Lúc này homeController không còn bị null nữa)
                if (homeController != null && homeController.getBorderpaneHome() != null) {
                    homeController.getBorderpaneHome().setCenter(mainView);
                }

                // 5. HIỂN THỊ LÊN STAGE
                Stage prStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
                Scene homeScene = new Scene(homeRoot);
                prStage.setScene(homeScene);

                prStage.setMaximized(true);
                prStage.show();

            } else {
                announcement.setText("Mật khẩu hoặc email không hợp lệ!");
            }
        }
    }
}