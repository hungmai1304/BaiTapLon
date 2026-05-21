package com.auction.client.controller.mainHome;

import com.auction.client.utils.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class WonAuctionsController {

    @FXML private TableView<?> wonAuctionsTable;
    @FXML private TableColumn<?, ?> colProductName;
    @FXML private TableColumn<?, ?> colProductImage;
    @FXML private TableColumn<?, ?> colSeller;
    @FXML private TableColumn<?, ?> colFinalPrice;
    @FXML private TableColumn<?, ?> colTime;
    @FXML private TableColumn<?, ?> colDelivery;

    @FXML
    public void initialize() {
        // Logic to populate table will go here
    }

    @FXML
    public void handleBack(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/main.fxml");
    }
}
