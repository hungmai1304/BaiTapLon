package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.network.NetworkClient;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction; // Import thêm Auction
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
        RequestSender.send("TIK_TOK_LISTENER_REQUEST",null);
        RequestSender.send("GET_ACTIVE_AUCTIONS_REQUEST",null);
        // Ưu tiên hiển thị Auction vì đây là giao kèo mới với Server
        renderCurrentAuction();
    }

    // --- SỬA LOGIC RENDER: Ưu tiên Auction trước ---
    private void renderCurrentAuction() {
        Auction currentAuction = ClientContext.getInstance().getCurrentAuction();
        if (currentAuction != null) {
            updateUI(currentAuction);
        } else {
            // Nếu chưa có Auction thì thử render Product cũ (nếu còn dùng)
            Product currentProduct = ClientContext.getInstance().getCurrentProduct();
            if (currentProduct != null) {
                updateUI(currentProduct);
            }
        }
    }

    public void updateUI(Product product) {
        Platform.runLater(() -> {
            if (product != null) {
                name.setText(product.getName());
                price.setText(String.format("%,.0f VNĐ", product.getStartPrice()));
                step.setText(String.format("Bước giá: %,.0f VNĐ", product.getStepPrice()));
            }
        });
    }

    public void updateUI(Auction auction) {
        Platform.runLater(() -> {
            if (auction != null && auction.getItem() != null) {
                Product product = (Product) auction.getItem();
                name.setText(product.getName());
                // Giá lấy realtime từ phiên đấu giá (Auction)
                price.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
                step.setText(String.format("Bước giá: %,.0f VNĐ", product.getStepPrice()));
            }
        });
    }

    @FXML
    public void handleUp(ActionEvent event) {
        boolean hasPrev = ClientContext.getInstance().previous();
        if (hasPrev) {
            renderCurrentAuction(); // Đổi sang render Auction
        } else {
            System.out.println("⚠️ Đã ở đầu danh sách!");
        }
    }

    @FXML
    public void handleDown(ActionEvent event) {
        boolean hasNext = ClientContext.getInstance().next();
        if (hasNext) {
            renderCurrentAuction(); // Đổi sang render Auction
        } else {
            System.out.println("? Chạm biên! Yêu cầu lấy thêm phiên đấu giá mới...");

            // --- SỬA LẠI TYPE REQUEST THEO GIAO KÈO 1 ---
            // Đổi từ GET_AUCTION_PRODUCT_REQUEST sang GET_ACTIVE_AUCTIONS_REQUEST
            Request auctionRequest = new Request(MessageType.GET_ACTIVE_AUCTIONS_REQUEST);

            NetworkClient.sendCommand(gson.toJson(auctionRequest));
            // ------------------------------------------
        }
    }

    public void cleanup() {
        ControllerRegistry.unregister("TikTokAuctionController");
        RequestSender.send("STOP_TIK_TOK_LISTENER_REQUEST", null);
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