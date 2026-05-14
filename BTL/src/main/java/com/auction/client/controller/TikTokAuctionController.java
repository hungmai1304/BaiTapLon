package com.auction.client.controller;

import com.auction.client.utils.ClientContext;
import com.auction.client.network.NetworkClient;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
import com.auction.protocol.Request;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import static com.auction.client.network.ClientMessageDispatcher.gson;

public class TikTokAuctionController {

    @FXML private Label name;
    @FXML private Label price;
    @FXML private Label step;

    @FXML
    public void initialize() {
        ControllerRegistry.register("TikTokAuctionController", this);
        // Hiển thị sản phẩm đầu tiên nếu đã có dữ liệu
        renderCurrentProduct();
    }

    private void renderCurrentProduct() {
        Product current = ClientContext.getInstance().getCurrentProduct();
        if (current != null) {
            updateUI(current);
        }
    }

    public void updateUI(Product product) {
        Platform.runLater(() -> {
            if (product != null) {
                name.setText(product.getName());
                price.setText(String.format("%,.0f VNĐ", product.getCurrentPrice()));
                step.setText(String.format("Bước giá: %,.0f VNĐ", product.getStepPrice()));
            }
        });
    }

    @FXML
    public void handleUp(ActionEvent event) {
        boolean hasPrev = ClientContext.getInstance().previous();
        if (hasPrev) {
            renderCurrentProduct();
        } else {
            System.out.println("⚠️ Đã ở đầu danh sách!");
            // Nếu muốn lướt vòng tròn hoặc lấy cũ hơn thì gửi command ở đây
            // NetworkClient.sendCommand("GET_PREVIOUS_PAGE");
        }
    }

    @FXML
    public void handleDown(ActionEvent event) {
        boolean hasNext = ClientContext.getInstance().next();
        if (hasNext) {
            renderCurrentProduct();
        } else {
            System.out.println("? Chạm biên! Yêu cầu lấy thêm sản phẩm mới...");

            // --- SỬA TẠI ĐÂY ---
            // Tạo object Request thay vì gửi String thuần
            Request auctionRequest = new Request(MessageType.GET_AUCTION_PRODUCT_REQUEST);

            // Chuyển sang JSON rồi mới gửi
            NetworkClient.sendCommand(gson.toJson(auctionRequest));
            // -------------------
        }
    }

    public void cleanup() {
        ControllerRegistry.unregister("TikTokAuctionController");
    }
    @FXML
    public void handleBidding(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/biddingBoard.fxml");
    }

    @FXML
    public void handleBotBid(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/botBidding.fxml");
    }
}