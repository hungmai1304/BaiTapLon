package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.MessageType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class TikTokAuctionController {

    @FXML private Label name;
    @FXML private Label price;
    @FXML private Label step;

    @FXML
    public void initialize() {
        ControllerRegistry.register("TikTokAuctionController", this);

        // Đăng ký listener và lấy danh sách đấu giá mới nhất
        RequestSender.send("TIK_TOK_LISTENER_REQUEST", null);
        RequestSender.send(MessageType.GET_ACTIVE_AUCTIONS_REQUEST, null);

        renderCurrentAuction();
    }

    /**
     * Chỉ lấy dữ liệu từ danh sách Auction duy nhất trong ClientContext
     */
    public void renderCurrentAuction() {
        // Lấy phiên đấu giá hiện tại từ danh sách activeAuctions
        Auction currentAuction = ClientContext.getInstance().getCurrentAuction();

        if (currentAuction != null) {
            updateUI(currentAuction);
        } else {
            // Nếu danh sách trống, hiển thị trạng thái chờ
            Platform.runLater(() -> {
                name.setText("Đang đợi sản phẩm...");
                price.setText("0 VNĐ");
                step.setText("Bước giá: 0 VNĐ");
            });
        }
    }

    /**
     * Cập nhật UI dựa trên đối tượng Auction và Product nằm bên trong nó
     */
    public void updateUI(Auction auction) {
        Platform.runLater(() -> {
            if (auction != null && auction.getItem() instanceof Product) {
                Product product = (Product) auction.getItem();
                name.setText(product.getName());
                // Giá hiển thị là giá hiện tại của phiên (Current Price)
                price.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
                step.setText(String.format("Bước giá: %,.0f VNĐ", product.getStepPrice()));
            }
        });
    }

    @FXML
    public void handleUp(ActionEvent event) {
        // Di chuyển lùi trong danh sách duy nhất
        boolean hasPrev = ClientContext.getInstance().previousAuction();
        if (hasPrev) {
            renderCurrentAuction();
        } else {
            System.out.println("[TikTokController] Đã ở đầu danh sách!");
        }
    }

    @FXML
    public void handleDown(ActionEvent event) {
        // Di chuyển tới trong danh sách duy nhất
        boolean hasNext = ClientContext.getInstance().nextAuction();
        if (hasNext) {
            renderCurrentAuction();
        } else {
            System.out.println("[TikTokController] Hết danh sách! Đang tải thêm...");
            RequestSender.send(MessageType.GET_ACTIVE_AUCTIONS_REQUEST, null);
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