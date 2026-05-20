package com.auction.client.controller.tiktok;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.client.controller.general.SomeGlobal; // Đảm bảo đã import đúng class này
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

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
    }

    @FXML
    public void handlePlaceBidForAuction(ActionEvent event) {
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
}