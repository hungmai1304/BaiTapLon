package com.auction.client.controller;

import static com.auction.client.utils.NavigationService.navigate;
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
        navigate(
                "/com/auction/client/view/settings.fxml",
                "Settings",
                true
        );
    }
}
