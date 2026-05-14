package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
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
 * Được Hùng và team sử dụng cho đồ án Java Socket.
 */
public class BiddingController {

    // --- 1. KHAI BÁO CÁC THÀNH PHẦN GIAO DIỆN (@FXML) ---
    @FXML private Label lblProductName;
    @FXML private Label lblStartPrice;
    @FXML private Label lblPriceStep;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLeaderName;
    @FXML private Label lblLeaderPrice;
    @FXML private Label lblNotification;
    @FXML private TextField txtBidAmount;
    @FXML private LineChart<String, Number> priceChart;

    // --- 2. KHAI BÁO DỮ LIỆU ---
    private Auction currentAuctionData;
    private XYChart.Series<String, Number> priceSeries = new XYChart.Series<>();
    private int bidCount = 0;

    // --- 3. HÀM KHỞI TẠO (Tự động chạy khi load FXML) ---
    @FXML
    public void initialize() {
        // Đăng ký controller để các Handler có thể tìm thấy và cập nhật UI
        ControllerRegistry.register("BiddingController", this);

        // Lấy dữ liệu phiên đấu giá hiện tại từ Context
        currentAuctionData = ClientContext.getInstance().getCurrentAuction();

        if (currentAuctionData != null && currentAuctionData.getItem() != null) {
            Product p = (Product) currentAuctionData.getItem();

            // Đổ dữ liệu lên các nhãn hiển thị
            lblProductName.setText("Tên sản phẩm: " + p.getName());
            lblStartPrice.setText("Giá khởi điểm: " + String.format("%,.0fđ", p.getStartPrice()));
            lblPriceStep.setText("Bước giá tối thiểu: " + String.format("%,.0fđ", p.getStepPrice()));
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", currentAuctionData.getCurrentPrice()));

            // Cập nhật người dẫn đầu (nếu đã có người ra giá)
            updateLeaderUI(currentAuctionData.getLeaderName(), currentAuctionData.getCurrentPrice());
        }

        // Cấu hình biểu đồ đường (LineChart)
        if (priceSeries.getName() == null) {
            priceSeries.setName("Giá đấu");
            priceChart.getData().add(priceSeries);
        }
    }

    // --- 4. XỬ LÝ SỰ KIỆN KHI BẤM NÚT "XÁC NHẬN RA GIÁ" ---
    // --- 4. XỬ LÝ SỰ KIỆN KHI BẤM NÚT "XÁC NHẬN RA GIÁ" ---
    @FXML
    public void handlePlaceBidForAuction(ActionEvent event) {
        try {
            String input = txtBidAmount.getText().trim();
            if (input.isEmpty()) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;"); // Màu đỏ cho lỗi
                lblNotification.setText("⚠️ Vui lòng nhập giá tiền!");
                return;
            }

            double bidAmount = Double.parseDouble(input);
            double currentPrice = currentAuctionData.getCurrentPrice();
            Product p = (Product) currentAuctionData.getItem();
            double stepPrice = p.getStepPrice();

            // Kiểm tra: Giá mới phải lớn hơn giá hiện tại
            if (bidAmount <= currentPrice) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("⚠️ Giá phải cao hơn giá hiện hành!");
                return;
            }

            // Kiểm tra: Bước giá phải là bội số
            if ((bidAmount - currentPrice) % stepPrice != 0) {
                lblNotification.setStyle("-fx-text-fill: #e74c3c;");
                lblNotification.setText("⚠️ Sai bước giá! Phải là bội số của " + String.format("%,.0fđ", stepPrice));
                return;
            }

            // Gửi yêu cầu đấu giá lên Server
            String email = SomeGlobal.getCurrentUser().getEmail();
            RequestSender.sendPlaceBidRequest(p.getId(), bidAmount, email);

            // CẬP NHẬT TRẠNG THÁI THÀNH CÔNG Ở ĐÂY
            lblNotification.setStyle("-fx-text-fill: #2ecc71;"); // Màu xanh lá thành công
            lblNotification.setText("✅ Đặt giá thành công! Đang chờ hệ thống ghi nhận...");

            txtBidAmount.clear();

        } catch (NumberFormatException e) {
            lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            lblNotification.setText("⚠️ Vui lòng nhập con số hợp lệ!");
        } catch (Exception e) {
            lblNotification.setStyle("-fx-text-fill: #e74c3c;");
            lblNotification.setText("⚠️ Lỗi kết nối server!");
        }
    }

    // --- 5. CẬP NHẬT UI KHI CÓ BROADCAST TỪ SERVER ---
    public void updateAuctionPriceRealtime(double newPrice, String leaderName) {
        Platform.runLater(() -> {
            // Cập nhật nhãn giá và người dẫn đầu
            lblCurrentPrice.setText("Giá hiện tại: " + String.format("%,.0fđ", newPrice));
            updateLeaderUI(leaderName, newPrice);

            // Cập nhật dữ liệu nội bộ
            if (currentAuctionData != null) {
                currentAuctionData.setCurrentPrice(newPrice);
                currentAuctionData.setLeaderName(leaderName);
            }

            // Vẽ thêm điểm mới lên biểu đồ diễn biến giá
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
        lblNotification.setText("Đã xóa ô nhập!");
    }

    @FXML
    public void handleBackToTikTok(ActionEvent event) {
        // Hủy đăng ký trước khi thoát để tránh rò rỉ bộ nhớ
        ControllerRegistry.unregister("BiddingController");
        NavigationService.setCenterView("/com/auction/client/view/tiktokAuction.fxml");
    }
}