package com.auction.client.controller;

import com.auction.client.NetworkClient;
import com.auction.common.model.Product;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import com.google.gson.Gson;

public class TikTokAuctionController {

    @FXML
    private Label name;
    @FXML
    private Label price;
    @FXML
    private Label step;

    private Gson gson = new Gson();

    // Hàm này tự động chạy khi giao diện FXML được load lên
    @FXML
    public void initialize() {
        // Cắm "ống nghe" túc trực chờ tin từ Server
        NetworkClient.setListener(message -> {
            // 🔥 BẮT BUỘC: Vì WebSocket chạy luồng riêng, phải dùng Platform.runLater
            // để nhờ luồng chính của JavaFX cập nhật Giao diện, nếu không sẽ bị Crash UI!
            Platform.runLater(() -> {
                if (message.startsWith("ERROR")) {
                    System.out.println("⚠️ Lỗi từ Server: " + message);
                    // Sau này bạn có thể làm Alert (thông báo đỏ) hiện ra ở đây
                } else {
                    try {
                        Product p = gson.fromJson(message, Product.class);
                        setCurrentAuction(p);
                        System.out.println("Đã cập nhật màn hình cho sản phẩm: " + p.getName());
                    } catch (Exception e) {
                        System.err.println("❌ Lỗi đọc dữ liệu JSON: " + e.getMessage());
                    }
                }
            });
        });
    }

    public void setCurrentAuction(Product product) {
        if (product != null) {
            name.setText(product.getName());
            // Format lại số cho đẹp nếu cần, ở đây mình tạm in số thực
            price.setText(String.valueOf(product.getCurrentPrice()));
            step.setText(String.valueOf(product.getStepPrice()));
        }
    }

    // --- CÁC NÚT BẤM BÂY GIỜ CHỈ LÀM NHIỆM VỤ GỬI LỆNH ---

    @FXML
    public void handleUp(ActionEvent event){
        System.out.println("Đang lướt video LÊN...");
        // Chỉ gửi lệnh đi, hàm initialize() ở trên sẽ lo việc bắt kết quả
        NetworkClient.sendCommand("GET_BACK");
    }

    @FXML
    public void handleDown(ActionEvent event){
        System.out.println("Đang lướt video XUỐNG...");
        // Nếu Server của bạn có lệnh GET_NEXT để sang video tiếp theo thì đổi ở đây nhé
        NetworkClient.sendCommand("GET_CURRENT");
    }

    // BONUS: Hàm nút "Trả Giá" (Bạn có thể gán vào nút Bid trên FXML)
    @FXML
    public void handleBid(ActionEvent event) {
        try {
            // Lấy giá hiện tại + bước giá để ra mức giá mới cần trả
            double current = Double.parseDouble(price.getText());
            double stepPrice = Double.parseDouble(step.getText());
            double newBidAmount = current + stepPrice;

            System.out.println("Đang gửi lệnh trả giá: " + newBidAmount);
            NetworkClient.sendCommand("BID:" + newBidAmount);

            // Bấm xong không cần làm gì thêm! Server nhận lệnh, nếu thành công
            // nó sẽ broadcast giá mới cho TẤT CẢ mọi người.
            // Hàm initialize() của bạn (và của người khác) sẽ tự động bắt được và nhảy số.

        } catch (NumberFormatException e) {
            System.err.println("Lỗi tính toán giá tiền!");
        }
    }
}