package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction; // Thêm Auction
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

    private Auction currentAuction; // Chuyển từ quản lý Product sang Auction
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();

    @FXML
    public void initialize() {
        // Đăng ký để các Handler có thể tìm thấy Controller này và cập nhật giá real-time
        ControllerRegistry.register("BotBiddingController", this);

        // Lấy Auction hiện tại từ danh sách duy nhất
        currentAuction = ClientContext.getInstance().getCurrentAuction();

        if (currentAuction != null && currentAuction.getItem() instanceof Product) {
            Product product = (Product) currentAuction.getItem();

            lblProductName.setText("Tên sản phẩm: " + product.getName());
            lblStartPrice.setText("Giá khởi điểm: " + String.format("%,.0f VNĐ", product.getStartPrice()));
            lblPriceStep.setText("Bước giá hệ thống: " + String.format("%,.0f VNĐ", product.getStepPrice()));

            // Giá hiện tại lấy từ Auction (vì đấu giá giá nhảy liên tục)
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", currentAuction.getCurrentPrice()));
        }

        priceSeries.setName("Lịch sử giá");
        priceChart.getData().add(priceSeries);
    }

    /**
     * Cập nhật giá hiển thị khi có thông báo từ Server
     */
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

                // Giả sử mày có User trong ClientContext hoặc Session
                // Chỗ SomeGlobal này thay bằng class User của mày (ví dụ: SessionManager)
                // String email = SessionManager.getInstance().getCurrentUser().getEmail();

                // Ở đây tao truyền mẫu, mày thay bằng User thực tế của mày
                String email = "bot_user@auction.com";

                // Gửi request cho Server để đăng ký Auto-bid
                RequestSender.sendRegisterBotRequest(
                        String.valueOf(currentAuction.getId()),
                        maxPrice,
                        botStep,
                        email
                );

                lblBotNotification.setText("Đã đăng ký Bot thành công!");
            }
        } catch (NumberFormatException e) {
            lblBotNotification.setText("Giá phải là số!");
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
        // Hủy đăng ký Controller trước khi chuyển cảnh
        ControllerRegistry.unregister("BotBiddingController");
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }
}