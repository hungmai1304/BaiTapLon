package com.auction.client.controller;

import static com.auction.client.utils.NavigationService.navigate;

import com.auction.client.utils.NavigationService;
import javafx.fxml.Initializable;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class TermsController implements Initializable {

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    public void handleBack(ActionEvent event) throws IOException {
        NavigationService.setCenterView("/com/auction/client/view/Settings.fxml");
    }
}
