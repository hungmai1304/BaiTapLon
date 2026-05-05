package com.auction.client.controller;

import com.auction.client.NetworkClient;
import com.auction.common.model.product.Product;
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
    // đọc String từ server thành các đối tượng mong muốn
    private Gson gson = new Gson();


    // Hàm này tự động chạy khi giao diện tiktok được load lên
    // nơi load kết quả nếu được server trả về, implement: MessageListener--> có khả năng nhận tin nhắn
    // nếu kết quả là lỗi: báo lỗi
    // nếu khác, thử ép nó sang dạng product
    // set product lên màn hình
    // nếu lỗi : báo lỗi

    @FXML
    public void initialize() {
        NetworkClient.setListener(message -> {

            Platform.runLater(() -> {
                if (message.startsWith("ERROR")) {
                    System.out.println("⚠️ Lỗi từ Server: " + message);

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
    // hàm set thông tin đấu giá hiện tại lên trên màn hình, chỉ nhận vào product
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
    public void handleUp(ActionEvent event) {
        System.out.println("Đang lướt video LÊN...");
        // Chỉ gửi lệnh đi, hàm initialize() ở trên sẽ lo việc bắt kết quả
        NetworkClient.sendCommand("GET_BACK");
    }

    @FXML
    public void handleDown(ActionEvent event) {
        System.out.println("Đang lướt video XUỐNG...");
        // Nếu Server của bạn có lệnh GET_NEXT để sang video tiếp theo thì đổi ở đây nhé
        NetworkClient.sendCommand("GET_NEXT");
    }
}