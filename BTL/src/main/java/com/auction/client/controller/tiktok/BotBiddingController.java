package com.auction.client.controller.tiktok;

import com.auction.client.controller.general.SomeGlobal; // Import SomeGlobal để lấy thông tin session
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction;
import com.auction.common.model.user.User;
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

    private Auction currentAuction;
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    @FXML
    public void initialize() {
        ControllerRegistry.register("BotBiddingController", this);

        currentAuction = ClientContext.getInstance().getCurrentAuction();

        if (currentAuction != null && currentAuction.getProduct() instanceof Product) {
            Product product = (Product) currentAuction.getProduct();

            lblProductName.setText("Tên sản phẩm: " + product.getName());
            lblStartPrice.setText("Giá khởi điểm: " + String.format("%,.0f VNĐ", product.getStartPrice()));
            lblPriceStep.setText("Bước giá hệ thống: " + String.format("%,.0f VNĐ", product.getStepPrice()));
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", currentAuction.getCurrentPrice()));

            // =========================================================================
            // NGHIỆP VỤ: Đóng băng ô nhập liệu nếu User hiện tại (từ SomeGlobal) chính là Seller
            // =========================================================================
            User currentUser = SomeGlobal.getCurrentUser();
            if (currentUser != null && product.getOwner() != null) {
                if (currentUser.getEmail().equalsIgnoreCase(product.getOwner().getEmail())) {
                    txtMaxBidPrice.setDisable(true);
                    txtBotPriceStep.setDisable(true);
                    lblBotNotification.setText("Bạn là người bán sản phẩm này, không có quyền đặt Bot!");
                    lblBotNotification.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        }

        priceSeries.setName("Lịch sử giá");
        priceChart.getData().add(priceSeries);
    }

    public void updatePrice(double newPrice) {
        Platform.runLater(() -> {
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", newPrice));
            if (currentAuction != null) {
                currentAuction.setCurrentPrice(newPrice);
            }
        });
    }

    @FXML
    public void handleRegisterBot(ActionEvent event) {
        try {
            // Chặn click gửi gói tin cố ý từ Client
            if (currentAuction != null && currentAuction.getProduct() instanceof Product) {
                Product product = (Product) currentAuction.getProduct();
                User currentUser = SomeGlobal.getCurrentUser();

                if (currentUser != null && product.getOwner() != null
                        && currentUser.getEmail().equalsIgnoreCase(product.getOwner().getEmail())) {
                    lblBotNotification.setText("Bạn không có quyền đăng ký Bot cho sản phẩm của chính mình!");
                    lblBotNotification.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                    System.out.println("[Client Block] Từ chối người bán tự cài Bot qua SomeGlobal.");
                    return;
                }
            }

            String maxInput = txtMaxBidPrice.getText().trim();
            String stepInput = txtBotPriceStep.getText().trim();

            if (maxInput.isEmpty() || stepInput.isEmpty()) {
                lblBotNotification.setText("Vui lòng nhập đầy đủ!");
                return;
            }

            double maxPrice = Double.parseDouble(maxInput);
            double botStep = Double.parseDouble(stepInput);

            if (currentAuction != null) {
                if (maxPrice <= currentAuction.getCurrentPrice()) {
                    lblBotNotification.setText("Giá tối đa phải cao hơn giá hiện tại!");
                    return;
                }

                // Trích xuất Email thực tế từ phiên làm việc tập trung SomeGlobal
                String email = "bot_user@auction.com";
                if (SomeGlobal.getCurrentUser() != null) {
                    email = SomeGlobal.getCurrentUser().getEmail();
                }

                RequestSender.sendRegisterBotRequest(
                        currentAuction.getProduct().getId(),
                        maxPrice,
                        botStep,
                        email
                );

                lblBotNotification.setText("Đã gửi yêu cầu đăng ký Bot lên Server...");
                lblBotNotification.setStyle("-fx-text-fill: green;");
            }
        } catch (NumberFormatException e) {
            lblBotNotification.setText("Giá phải là số!");
            lblBotNotification.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        // Nếu là người bán thì đóng băng, không cho dùng tính năng Clear
        if (currentAuction != null && currentAuction.getProduct() instanceof Product) {
            Product product = (Product) currentAuction.getProduct();
            User currentUser = SomeGlobal.getCurrentUser();
            if (currentUser != null && product.getOwner() != null
                    && currentUser.getEmail().equalsIgnoreCase(product.getOwner().getEmail())) {
                return;
            }
        }
        txtMaxBidPrice.clear();
        txtBotPriceStep.clear();
        lblBotNotification.setText("Trạng thái: Chưa đăng ký");
        lblBotNotification.setStyle("-fx-text-fill: black;");
    }

    @FXML
    public void handleBackToTikTok(ActionEvent event) {
        ControllerRegistry.unregister("BotBiddingController");
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }
}