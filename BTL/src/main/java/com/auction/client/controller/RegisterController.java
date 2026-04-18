package com.auction.client.controller;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class RegisterController {

    // Phải có @FXML để JavaFX hiểu và ánh xạ với file fxml
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
    public void handleConfirm(ActionEvent event)throws IOException{
        // nếu mà bấm vào nút này thì sẽ check :
        //1. email tồn tại
        //2. Tên chưa từng bị trùng trong database
        //3. mật khẩu trên 6 số
        //4. reconfirm trùng khớp với mật khẩu
        String registerName=boxfield_register_name.getText();
        String registerEmail=textfield_register_email.getText();
        String password=password_register_pass.getText();
        String reconfirm=password_register_reconfirm.getText();

        // Phần thử nghiệm và cần được viết lại theo cách gọi hàm từ server để lấy key
        if (!password.equals(reconfirm)){
            announcement.setText("Mật khẩu không trùng khớp!");
        }
        else if(!registerEmail.contains("@")){
            announcement.setText("Email Không Hợp Lệ!");
        }
        else{
            announcement.setText("Đăng kí thành công, mời quay lại trang đăng nhập!");
        }

    }
}