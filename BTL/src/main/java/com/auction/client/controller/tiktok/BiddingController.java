package com.auction.client.controller.tiktok;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.client.controller.general.SomeGlobal;
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import java.time.LocalDateTime;

public class BiddingController {

    @FXML private Label lblProductName;
    @FXML private Label lblProductDesc;
    @FXML private Label lblStartPrice;
    @FXML private Label lblPriceStep;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeaderName;
    @FXML private Label lblLeaderPrice;
    @FXML private Label lblNotification;
    @FXML private TextField txtBidAmount;
    @FXML private LineChart<String, Number> priceChart;

    @FXML private Label lblCountdown;
    @FXML private Button btnPlaceBid;
    private static Timeline countdownTimeline;

    private Auction currentAuctionData;
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private int bidCount = 0;

    @FXML
    public void initialize() {
        ControllerRegistry.register("BiddingController", this);

        currentAuctionData = ClientContext.getInstance().getCurrentAuction();

        if (currentAuctionData != null && currentAuctionData.getProduct() != null) {
            Product p = (Product) currentAuctionData.getProduct();

            lblProductName.setText("Tên sản phẩm: " + p.getName());

            if (lblProductDesc != null) {
                String desc = p.getDescription();
                lblProductDesc.setText("Mô tả: " + (desc != null && !desc.isEmpty() ? desc : "Không có mô tả"));
            }

            lblStartPrice.setText("Giá khởi điểm: " + String.format("%,.0fđ", p.getStartPrice()));
            lblPriceStep.setText("Bước giá tối thiểu: " + String.format("%,.0fđ", p.getStepPrice()));
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", currentAuctionData.getCurrentPrice()));

            updateLeaderUI(currentAuctionData.getLeaderName(), currentAuctionData.getCurrentPrice());
        } else {
            lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            lblNotification.setText("Lỗi: Không tìm thấy thông tin phiên đấu giá!");
        }

        if (priceSeries.getName() == null) {
            priceSeries.setName("Giá đấu");
            priceChart.getData().add(priceSeries);
        }

        startAuctionCountdown();
    }

    @FXML
    public void handlePlaceBidForAuction(ActionEvent event) {
        if (currentAuctionData != null && currentAuctionData.getProduct() != null) {
            Product p = (Product) currentAuctionData.getProduct();
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime fixedStart = p.getStartTime() != null ? p.getStartTime().plusHours(7) : null;
            LocalDateTime fixedEnd = p.getEndTime() != null ? p.getEndTime().plusHours(7) : null;

            if (fixedStart != null && now.isBefore(fixedStart)) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Đang trong thời gian quảng cáo, không thể ra giá!");
                return;
            }
            if (fixedEnd != null && now.isAfter(fixedEnd)) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Phiên đấu giá đã kết thúc!");
                return;
            }
        }

        try {
            if (currentAuctionData == null || currentAuctionData.getProduct() == null) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Lỗi dữ liệu! Vui lòng quay lại màn hình trước.");
                return;
            }

            String input = txtBidAmount.getText().trim();
            if (input.isEmpty()) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Vui lòng nhập giá tiền!");
                return;
            }

            double bidAmount = Double.parseDouble(input);
            double currentPrice = currentAuctionData.getCurrentPrice();
            Product p = (Product) currentAuctionData.getProduct();
            double stepPrice = p.getStepPrice();

            if (bidAmount <= currentPrice) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Giá phải cao hơn giá hiện hành!");
                return;
            }

            if ((bidAmount - currentPrice) < stepPrice) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Sai bước giá! Giá đặt mới phải cao hơn giá cũ tối thiểu là " + String.format("%,.0fđ", stepPrice));
                return;
            }

            String email = SomeGlobal.getCurrentUser().getEmail();
            RequestSender.sendPlaceBidRequest(p.getId(), bidAmount, email);

            lblNotification.setStyle("-fx-text-fill: #f39c12;");
            lblNotification.setText("Đang kiểm tra ví và gửi yêu cầu...");
            txtBidAmount.clear();


        } catch (NumberFormatException e) {
            lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            lblNotification.setText("Vui lòng nhập con số hợp lệ!");
        } catch (Exception e) {
            lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            lblNotification.setText("Lỗi hệ thống: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    public void showServerNotification(String msg, boolean isSuccess) {
        Platform.runLater(() -> {
            lblNotification.setStyle(isSuccess ? "-fx-text-fill: #2ecc71;" : "-fx-text-fill: #e74c3c;");
            lblNotification.setText(msg);
        });
    }

    public void updateAuctionPriceRealtime(double newPrice, String leaderName) {
        Platform.runLater(() -> {
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", newPrice));
            updateLeaderUI(leaderName, newPrice);

            if (currentAuctionData != null) {
                currentAuctionData.setCurrentPrice(newPrice);
                currentAuctionData.setLeaderName(leaderName);
            }

            bidCount++;
            priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));
        });
    }

    private void updateLeaderUI(String name, double price) {
        if (name != null && !name.isEmpty()) {
            lblLeaderName.setText(name);
            lblLeaderPrice.setText("- " + String.format("%,.0fđ", price));
            // KIỂM TRA XEM MÌNH CÓ CÒN DẪN ĐẦU KHÔNG
            if (SomeGlobal.getCurrentUser() != null) {
                String myEmail = SomeGlobal.getCurrentUser().getEmail();
                String myUsername = SomeGlobal.getCurrentUser().getUsername();
                // Nếu người dẫn đầu hiện tại KHÔNG PHẢI là email hoặc username của mình
                if (!name.trim().equals(myEmail) && !name.trim().equals(myUsername)) {
                    // Và trên màn hình vẫn còn sót lại chữ "dẫn đầu" từ lượt đặt giá trước
                    if (lblNotification.getText() != null && lblNotification.getText().contains("dẫn đầu")) {
                        // Xóa ngay đi để tránh gây hiểu lầm cho người dùng
                        lblNotification.setText("");
                    }
                }
            }
        } else {
            lblLeaderName.setText("Chưa có");
            lblLeaderPrice.setText("0đ");
        }
    }

    @FXML
    public void handleClear(ActionEvent event) {
        txtBidAmount.clear();
        lblNotification.setText("");
    }

    @FXML
    public void handleBackToTikTok(ActionEvent event) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        ControllerRegistry.unregister("BiddingController");
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }

    public void updateRealtimeBid(String productId, double newPrice, String leaderName) {
        Platform.runLater(() -> {
            if (this.currentAuctionData != null && this.currentAuctionData.getProduct().getId().equals(productId)) {
                lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", newPrice));
                updateLeaderUI(leaderName, newPrice);

                this.currentAuctionData.setCurrentPrice(newPrice);
                this.currentAuctionData.setLeaderName(leaderName);

                bidCount++;
                priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));

                System.out.println("[Bidding UI] Đã nhảy số và vẽ biểu đồ realtime!");
            }
        });
    }

    private void startAuctionCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }

        if (currentAuctionData == null || currentAuctionData.getProduct() == null) return;
        Product p = (Product) currentAuctionData.getProduct();

        countdownTimeline = new Timeline(new KeyFrame(javafx.util.Duration.millis(1000), event -> {
            LocalDateTime now = LocalDateTime.now();

            LocalDateTime startTime = p.getStartTime() != null ? p.getStartTime().plusHours(7) : null;
            LocalDateTime endTime = p.getEndTime() != null ? p.getEndTime().plusHours(7) : null;

            if (startTime == null || endTime == null) {
                if (lblCountdown != null) lblCountdown.setText("Không xác định được thời gian!");
                return;
            }

            if (now.isBefore(startTime)) {
                if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                long diff = java.time.Duration.between(now, startTime).getSeconds();
                if (diff < 0) diff = 0;
                if (lblCountdown != null) {
                    lblCountdown.setText(String.format("Đợi quảng cáo: %02d:%02d", diff / 60, diff % 60));
                    lblCountdown.setStyle("-fx-text-fill: #f39c12;");
                }
                if (lblNotification != null) {
                    lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                    lblNotification.setText("Phiên đấu giá chưa bắt đầu! Hãy xem thông tin sản phẩm.");
                }

            } else if (now.isAfter(startTime) && now.isBefore(endTime)) {
                if (btnPlaceBid != null) btnPlaceBid.setDisable(false);
                long diff = java.time.Duration.between(now, endTime).getSeconds();
                if (diff < 0) diff = 0;
                if (lblCountdown != null) {
                    lblCountdown.setText(String.format("Thời gian còn lại: %02d:%02d", diff / 60, diff % 60));
                    lblCountdown.setStyle("-fx-text-fill: #2ecc71;");
                }
                if (lblNotification != null && lblNotification.getText().contains("chưa bắt đầu")) {
                    lblNotification.setText("");
                }

            } else {
                if (btnPlaceBid != null) btnPlaceBid.setDisable(true);
                if (lblCountdown != null) {
                    lblCountdown.setText("Phiên đấu giá ĐÃ KẾT THÚC!");
                    lblCountdown.setStyle("-fx-text-fill: #95a5a6;");
                }
                if (lblNotification != null) {
                    lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                    lblNotification.setText("Hết giờ! Bạn không thể đưa ra mức giá nào nữa.");
                }
                countdownTimeline.stop();
            }
        }));

        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    public Label getLblProductName() {
        return this.lblProductName;
    }

    public com.auction.common.model.auction.Auction getCurrentAuctionData() {
        return this.currentAuctionData;
    }
}