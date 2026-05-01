package com.auction.client.controller;

import com.auction.client.NetworkClient;
import com.auction.common.model.Product;
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

import com.google.gson.Gson;
import java.io.IOException;

public class TikTokAuctionController {
    @FXML
    private Label name;
    @FXML
    private Label price;
    @FXML
    private Label step;




    public void setCurrentAuction(Product product) {
        if (product != null) {
            name.setText(product.getName());
            price.setText(String.valueOf(product.getCurrentPrice()));
            step.setText(String.valueOf(product.getStepPrice()));
        }
    }

    @FXML
    public void handleUp(ActionEvent event){
        // Gửi lệnh String, nhận về đối tượng Product
        Gson gson = new Gson();
        String jsonResponse = NetworkClient.sendRequest("GET_BACK");
        Product backProduct = gson.fromJson(jsonResponse, Product.class);

        if (backProduct != null) {
            setCurrentAuction(backProduct);
        } else {
            System.out.println("Không có sản phẩm tiếp theo hoặc lỗi kết nối.");
        }

    }
    @FXML
    public void handleDown(ActionEvent event){
        Gson gson = new Gson();
        String jsonResponse = NetworkClient.sendRequest("GET_CURRENT");
        Product nextProduct = gson.fromJson(jsonResponse, Product.class);

        if (nextProduct != null) {
            setCurrentAuction(nextProduct);
        } else {
            System.out.println("Không có sản phẩm tiếp theo hoặc lỗi kết nối.");
        }

    }




}
