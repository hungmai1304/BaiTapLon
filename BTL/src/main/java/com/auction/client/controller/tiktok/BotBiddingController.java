package com.auction.client.controller.tiktok;

import com.auction.client.controller.general.SomeGlobal;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import java.time.ZoneId;

public class BotBiddingController {

    @FXML private Label lblProductName;
    @FXML private Label lblStartPrice;
    @FXML private Label lblPriceStep;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblBotNotification;

    // Đã thêm ánh xạ UI cho tính năng Đồng hồ và Khóa nút
    @FXML private Label lblCountdown;
    @FXML private Button btnRegisterBot;

    @FXML private TextField txtMaxBidPrice;
    @FXML private TextField txtBotPriceStep;
    @FXML private LineChart<String, Number> priceChart;

    private Auction currentAuction;
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private static Timeline countdownTimeline;

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

            // NGHIỆP VỤ: Đóng băng ô nhập liệu nếu User hiện tại (từ SomeGlobal) chính là Seller
            User currentUser = SomeGlobal.getCurrentUser();
            if (currentUser != null && product.getOwner() != null) {
                if (currentUser.getEmail().equalsIgnoreCase(product.getOwner().getEmail())) {
                    txtMaxBidPrice.setDisable(true);
                    txtBotPriceStep.setDisable(true);
                    if (btnRegisterBot != null) btnRegisterBot.setDisable(true);
                    lblBotNotification.setText("Bạn là người bán sản phẩm này, không có quyền đặt Bot!");
                    lblBotNotification.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
                }
            }
        }

        priceSeries.setName("Lịch sử giá");
        String timeNowInit = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        priceSeries.getData().add(new XYChart.Data<>(timeNowInit, currentAuction.getCurrentPrice()));

        priceChart.getData().add(priceSeries);

        // Khởi động cỗ máy đếm ngược thời gian giống hệt màn hình Bidding
        startAuctionCountdown();
    }

    public void updatePrice(double newPrice) {
        Platform.runLater(() -> {
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0f VNĐ", newPrice));
            if (currentAuction != null) {
                currentAuction.setCurrentPrice(newPrice);
            }
            String timeNow = java.time.LocalTime.now().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
            priceSeries.getData().add(new XYChart.Data<>(timeNow, newPrice));

            // Dọn bớt điểm cũ đi nếu nó vẽ quá 20 lần để khỏi tràn màn hình
            if (priceSeries.getData().size() > 20) {
                priceSeries.getData().remove(0);
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

                lblBotNotification.setText("Đã đăng ký Bot thành công!");
                lblBotNotification.setStyle("-fx-text-fill: green;");
            }
        } catch (NumberFormatException e) {
            lblBotNotification.setText("Giá phải là số!");
            lblBotNotification.setStyle("-fx-text-fill: red;");
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
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

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        ControllerRegistry.unregister("BotBiddingController");
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }

    private void startAuctionCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (currentAuction == null || currentAuction.getProduct() == null) return;
        Product p = (Product) currentAuction.getProduct();

        countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.millis(1000), event -> {
            long nowMillis = System.currentTimeMillis();
            long startMillis = p.getStartTime() != null ? p.getStartTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;
            long endMillis = p.getEndTime() != null ? p.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() : 0;

            if (startMillis == 0 || endMillis == 0) {
                if (lblCountdown != null) lblCountdown.setText("Không xác định được thời gian!");
                return;
            }

            // Chỉ thay đổi trạng thái Disable của nút nếu user khong phải người bán
            User currentUser = SomeGlobal.getCurrentUser();
            boolean isSeller = (currentUser != null && p.getOwner() != null && currentUser.getEmail().equalsIgnoreCase(p.getOwner().getEmail()));

            if (nowMillis < startMillis) {
                // Trong thời gian quảng cáo sẽ khóa nút đăng ký Bot
                if (btnRegisterBot != null) btnRegisterBot.setDisable(true);
                long diffSeconds = (startMillis - nowMillis) / 1000;
                if (diffSeconds < 0) diffSeconds = 0;

                if (lblCountdown != null) {
                    lblCountdown.setText(String.format("Đợi quảng cáo: %02d:%02d", diffSeconds / 60, diffSeconds % 60));
                    lblCountdown.setStyle("-fx-text-fill: #f39c12;"); // Màu vàng cam
                }

            } else if (nowMillis >= startMillis && nowMillis < endMillis) {
                // Đang trong phiên đấu giá mở khóa nút đăng ký Bot
                if (btnRegisterBot != null) btnRegisterBot.setDisable(isSeller);

                long diffSeconds = (endMillis - nowMillis) / 1000;
                if (diffSeconds < 0) diffSeconds = 0;

                if (lblCountdown != null) {
                    lblCountdown.setText(String.format("Thời gian còn lại: %02d:%02d", diffSeconds / 60, diffSeconds % 60));
                    lblCountdown.setStyle("-fx-text-fill: #2ecc71;"); // Màu xanh lá
                }

            } else {
                // Phiên đấu giá kết thúc khóa vĩnh viễn nút đăng ký Bot
                if (btnRegisterBot != null) btnRegisterBot.setDisable(true);
                if (lblCountdown != null) {
                    lblCountdown.setText("Phiên đấu giá ĐÃ KẾT THÚC!");
                    lblCountdown.setStyle("-fx-text-fill: #e74c3c;"); // Màu đỏ
                }
                countdownTimeline.stop();
            }
        }));

        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }
}