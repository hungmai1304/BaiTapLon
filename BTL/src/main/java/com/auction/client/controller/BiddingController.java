package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.client.controller.SomeGlobal; // Đảm bảo đã import đúng class này
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

// --- CHỈ THÊM: Các thư viện bổ sung phục vụ đếm ngược ---
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import java.time.LocalDateTime;
// --- HẾT PHẦN THÊM THƯ VIỆN ---

/**
 * Controller quản lý màn hình đấu giá chi tiết.
 * Đã được tối ưu để tránh lỗi null và hiển thị thông báo chính xác.
 */
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

    // --- CHỈ THÊM: Thành phần UI điều khiển đếm ngược và khóa nút ---
    @FXML private Label lblCountdown;
    @FXML private Button btnPlaceBid;
    private static Timeline countdownTimeline;
    // --- HẾT PHẦN THÊM THÀNH PHẦN ---

    private Auction currentAuctionData;
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private int bidCount = 0;

    @FXML
    public void initialize() {
        ControllerRegistry.register("BiddingController", this);

        // Lấy dữ liệu phiên đấu giá hiện tại từ Context
        currentAuctionData = ClientContext.getInstance().getCurrentAuction();

        // KIỂM TRA DỮ LIỆU: Nếu TikTokController chưa lưu Auction vào Context, ta báo lỗi luôn
        if (currentAuctionData != null && currentAuctionData.getProduct() != null) {
            Product p = (Product) currentAuctionData.getProduct();

            lblProductName.setText("Tên sản phẩm: " + p.getName());
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

        // --- CHỈ THÊM: Kích hoạt cỗ máy đếm ngược tự động khi mở màn hình ---
        startAuctionCountdown();
        // --- HẾT PHẦN THÊM ---
    }

    @FXML
    public void handlePlaceBidForAuction(ActionEvent event) {
        // --- CHỈ THÊM: Chặn thô bạo bằng code nếu cố tình bấm nút lúc đang khóa (Đã xóa icon) ---
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
        // --- HẾT PHẦN THÊM ---

        try {
            // 1. KIỂM TRA DỮ LIỆU CỐT LÕI (Tránh lỗi văng đỏ lòm do null)
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

            // 2. KIỂM TRA LOGIC ĐẤU GIÁ
            if (bidAmount <= currentPrice) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Giá phải cao hơn giá hiện hành!");
                return;
            }

            if ((bidAmount - currentPrice) % stepPrice != 0) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("Sai bước giá! Phải tăng theo bội số của " + String.format("%,.0fđ", stepPrice));
                return;
            }

            // 3. GỬI YÊU CẦU LÊN SERVER
            String email = SomeGlobal.getCurrentUser().getEmail();
            RequestSender.sendPlaceBidRequest(p.getId(), bidAmount, email);

            //  đang chờ Server check ví!
            lblNotification.setStyle("-fx-text-fill: #f39c12;");
            lblNotification.setText("Đang kiểm tra ví và gửi yêu cầu...");
            txtBidAmount.clear();


        } catch (NumberFormatException e) {
            lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            lblNotification.setText("Vui lòng nhập con số hợp lệ!");
        } catch (Exception e) {
            // Đây là nơi bắt các lỗi kết nối thật sự
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

            // Vẽ biểu đồ tự động nhảy khi có giá mới
            bidCount++;
            priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));
        });
    }

    private void updateLeaderUI(String name, double price) {
        if (name != null && !name.isEmpty()) {
            lblLeaderName.setText(name);
            lblLeaderPrice.setText("- " + String.format("%,.0fđ", price));
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
        // --- CHỈ THÊM: Tắt bộ đếm thời gian chạy ngầm khi thoát màn hình để giải phóng RAM ---
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        // --- HẾT PHẦN THÊM ---
        ControllerRegistry.unregister("BiddingController");
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }
    public void updateRealtimeBid(String productId, double newPrice, String leaderName) {
        Platform.runLater(() -> {
            // 1. Kiểm tra ID:
            if (this.currentAuctionData != null && this.currentAuctionData.getProduct().getId().equals(productId)) {

                // 2. Cập nhật giá chữ to
                lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", newPrice));

                // 3. Cập nhật tên người dẫn đầu
                updateLeaderUI(leaderName, newPrice);

                // 4. Lưu vào RAM để lúc back ra back vào nó không bị mất
                this.currentAuctionData.setCurrentPrice(newPrice);
                this.currentAuctionData.setLeaderName(leaderName);

                // 5. Cập nhật biểu đồ
                bidCount++;
                priceSeries.getData().add(new XYChart.Data<>(String.valueOf(bidCount), newPrice));

                System.out.println("[Bidding UI] Đã nhảy số và vẽ biểu đồ realtime!");
            }
        });
    }

    // --- CHỈ THÊM: Hàm xử lý đếm ngược tự động chạy độc lập (Đã xóa toàn bộ icon) ---
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

            // TRẠNG THÁI 1: CHƯA ĐẾN GIỜ (Đang quảng cáo)
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

                // TRẠNG THÁI 2: ĐANG ĐẤU GIÁ
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

                // TRẠNG THÁI 3: HẾT GIỜ
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
    // --- HẾT PHẦN THÊM ---
}