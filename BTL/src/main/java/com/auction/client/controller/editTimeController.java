package com.auction.client.controller;

import com.auction.client.utils.NavigationService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class editTimeController {
    @FXML
    public  void handleBackClicked(ActionEvent event) throws IOException {
        NavigationService.setCenterView("/com/auction/client/view/ShopSell.fxml");
    }


    // sửa thông tin của sản phẩm được gọi
    // EDIT_PRODUCT_INFO_REQUEST
}
//