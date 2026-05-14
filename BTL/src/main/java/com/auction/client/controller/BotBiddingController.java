package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

public class BotBiddingController {

    @FXML private Label lblProductName;
    @FXML private Label lblStartPrice;
    @FXML private Label lblPriceStep;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblBotNotification;
    @FXML private TextField txtMaxBidPrice;
    @FXML private TextField txtBotPriceStep;
    @FXML private LineChart<String, Number> priceChart;

    private Product currentProduct;
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    @FXML
    public void initialize() {
        currentProduct = ClientContext.getInstance().getCurrentProduct();
        if (currentProduct != null) {
            lblProductName.setText("Tên sản phẩm: " + currentProduct.getName());
            lblStartPrice.setText("Giá khởi điểm: " + String.format("%,.0fđ", currentProduct.getStartPrice()));
            lblPriceStep.setText("Bước giá hệ thống: " + String.format("%,.0fđ", currentProduct.getStepPrice()));
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", currentProduct.getCurrentPrice()));
        }
        priceSeries.setName("Giá đấu");
        priceChart.getData().add(priceSeries);
    }

    public void updatePrice(double newPrice) {
        Platform.runLater(() -> {
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", newPrice));
            if (currentProduct != null) currentProduct.setCurrentPrice(newPrice);
        });
    }

    @FXML
    public void handleRegisterBot(ActionEvent event) {
        try {
            String maxInput = txtMaxBidPrice.getText().trim();
            String stepInput = txtBotPriceStep.getText().trim();
            if (maxInput.isEmpty() || stepInput.isEmpty()) {
                lblBotNotification.setText("⚠️ Vui lòng nhập đầy đủ!");
                return;
            }
            double maxPrice = Double.parseDouble(maxInput);
            double botStep = Double.parseDouble(stepInput);
            if (currentProduct != null && maxPrice <= currentProduct.getCurrentPrice()) {
                lblBotNotification.setText("⚠️ Giá tối đa phải cao hơn giá hiện tại!");
                return;
            }
            String email = SomeGlobal.getCurrentUser().getEmail();
            RequestSender.sendRegisterBotRequest(currentProduct.getId(), maxPrice, botStep, email);
            lblBotNotification.setText("✅ Đã đăng ký Bot thành công!");
        } catch (NumberFormatException e) {
            lblBotNotification.setText("⚠️ Giá phải là số!");
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        txtMaxBidPrice.clear();
        txtBotPriceStep.clear();
        lblBotNotification.setText("Trạng thái: Chưa đăng ký");
    }

    @FXML
    public void handleBackToTikTok(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }
}