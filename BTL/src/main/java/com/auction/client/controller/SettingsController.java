package com.auction.client.controller;

import com.auction.client.utils.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

import static com.auction.client.utils.NavigationService.navigate;

public class SettingsController {
    @FXML
    public void handleLogoutClicked(ActionEvent event) throws IOException {
        Parent loader= FXMLLoader.load(getClass().getResource("/com/auction/client/view/login.fxml"));
        Scene scene_login=new Scene(loader);
        //Lấy cửa số gốc
        Stage prStage=(Stage) ((Node) event.getSource()).getScene().getWindow();
        //đặt scene mới lên cửa sổ gốc
        prStage.setMaximized(false);
        prStage.setScene(scene_login);
        prStage.show();
    }
    @FXML
    public void handleTerms(ActionEvent event) throws IOException {
        NavigationService.setCenterView("/com/auction/client/view/terms.fxml");
    }
}
