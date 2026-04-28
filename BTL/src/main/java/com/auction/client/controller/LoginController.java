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
import com.auction.client.utils.ValidationUtils;


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

    // xử lí chuyển giao diện sang main tại đây
    @FXML
    public void handleGetText(ActionEvent event) throws IOException {
        String email = mail.getText().trim(); // Thêm .trim() để xóa khoảng trắng thừa
        String password = passwordtext.getText();

        // 1. Kiểm tra rỗng
        if (email.isEmpty() || password.isEmpty()) {
            announcement.setText("Vui lòng nhập đầy đủ thông tin!");
            return; // Thoát sớm để đỡ phải dùng else lồng nhau
        }

        // 2. Kiểm tra định dạng (Dùng hàm riêng đã tạo ở trên)
        if (!ValidationUtils.isValidCredentials(email, password)) {
            announcement.setText("Email không hợp lệ hoặc mật khẩu dưới 6 ký tự!");
            return;
        }

        // 3. Nếu mọi thứ OK -> Tiến hành chuyển hướng
        announcement.setText("Xin chào: " + email + " Đang chuyển hướng...");
        navigateToHome(event);
    }

    // Tách luôn đoạn chuyển hướng ra một hàm cho dễ quản lý
    private void navigateToHome(ActionEvent event) throws IOException {
        FXMLLoader homeLoader = new FXMLLoader(getClass().getResource("/com/auction/client/view/home.fxml"));
        Parent homeRoot = homeLoader.load();

        HomeController homeController = homeLoader.getController();
        SomeGlobal.storeHomeController(homeController);

        // Load vùng trung tâm
        StackPane mainView = FXMLLoader.load(getClass().getResource("/com/auction/client/view/main.fxml"));

        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(mainView);
        }

        // Hiển thị Stage
        Stage prStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        prStage.setScene(new Scene(homeRoot));
        prStage.show();
    }



}