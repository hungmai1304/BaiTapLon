package com.auction.client.controller;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
}