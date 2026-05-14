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

public class BiddingController {

    @FXML private Label lblProductName;
    @FXML private Label lblStartPrice;
    @FXML private Label lblPriceStep;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeaderName;
    @FXML private Label lblLeaderPrice;
    @FXML private Label lblNotification;
    @FXML private TextField txtBidAmount;
    @FXML private LineChart<String, Number> priceChart;

    private Product currentProduct;
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private int bidCount = 0;

    @FXML
    public void initialize() {
        ControllerRegistry.register("BiddingController", this);
        currentProduct = ClientContext.getInstance().getCurrentProduct();
        if (currentProduct != null) {
            lblProductName.setText("Tên sản phẩm: " + currentProduct.getName());
            lblStartPrice.setText("Giá khởi điểm: " + String.format("%,.0fđ", currentProduct.getStartPrice()));
            lblPriceStep.setText("Bước giá tối thiểu: " + String.format("%,.0fđ", currentProduct.getStepPrice()));
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", currentProduct.getCurrentPrice()));
        }
        // Setup chart
        priceSeries.setName("Giá đấu");
        priceChart.getData().add(priceSeries);
    }

    public void updatePrice(double newPrice, String leaderName) {
        Platform.runLater(() -> {
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", newPrice));
            lblLeaderName.setText(leaderName);
            lblLeaderPrice.setText("- " + String.format("%,.0fđ", newPrice));
            if (currentProduct != null) currentProduct.setCurrentPrice(newPrice);
            // Cập nhật chart
            bidCount++;
            priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));
        });
    }

    @FXML
    public void handlePlaceBid(ActionEvent event) {
        try {
            String input = txtBidAmount.getText().trim();
            if (input.isEmpty()) {
                lblNotification.setText("⚠️ Vui lòng nhập giá thầu!");
                return;
            }
            double bidAmount = Double.parseDouble(input);
            if (currentProduct != null && bidAmount <= currentProduct.getCurrentPrice()) {
                lblNotification.setText("⚠️ Giá phải cao hơn giá hiện tại!");
                return;
            }
            String email = SomeGlobal.getCurrentUser().getEmail();
            RequestSender.sendPlaceBidRequest(currentProduct.getId(), bidAmount, email);
            lblNotification.setText("✅ Đã gửi: " + String.format("%,.0fđ", bidAmount));
            txtBidAmount.clear();
        } catch (NumberFormatException e) {
            lblNotification.setText("⚠️ Giá thầu phải là số!");
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        txtBidAmount.clear();
        lblNotification.setText("Đã xóa giá tiền!");
    }

    @FXML
    public void handleBackToTikTok(ActionEvent event) {
        ControllerRegistry.unregister("BiddingController");
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }
}