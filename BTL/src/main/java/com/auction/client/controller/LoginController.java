package com.auction.client.controller;
import com.auction.server.service.AuctionServer;
import com.auction.server.service.AuctionServer;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;

import java.awt.*;
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
    public void handleGetText(ActionEvent event)throws IOException{
        String email=mail.getText();
        String password=passwordtext.getText();

        if(email.isEmpty() || password.isEmpty()){
            announcement.setText("Vui lòng nhập đầy đủ thông tin");
        }
        else{
            boolean isValid= AuctionServer.checkLogin(email,password);
            if(isValid){
                announcement.setText("Xin chào: "+email+" đang chuyển hướng");
                Parent loader= FXMLLoader.load(getClass().getResource("/com/auction/client/view/home.fxml"));
                Scene home=new Scene(loader);

                Stage prStage=(Stage) ((Node) event.getSource()).getScene().getWindow();
                prStage.setScene(home);
                prStage.show();

            }
            else{
                announcement.setText("mật khẩu hoặc email không hợp lệ!");
            }
        }

    }



}