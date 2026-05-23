package com.auction.client.controller.tiktok;

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
    @FXML private Label lblTopBidder;
    @FXML private javafx.scene.image.ImageView productImage;

    @FXML
    public void initialize() {
        ControllerRegistry.register("TikTokAuctionController", this);

        // Đăng ký listener và lấy danh sách đấu giá mới nhất
        RequestSender.send(MessageType.TIK_TOK_LISTENER_REQUEST, new java.util.HashMap<>());
        RequestSender.sendGetActiveAuctionsRequest();

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
            if (auction != null && auction.getProduct() instanceof Product) {
                Product product = (Product) auction.getProduct();
                name.setText(product.getName());
                // Giá hiển thị là giá hiện tại của phiên (Current Price)
                price.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
                step.setText(String.format("Bước giá: %,.0f VNĐ", product.getStepPrice()));
                if (lblTopBidder != null) {
                    // Kiểm tra xem đã có ai đặt giá chưa
                    String leader = (auction.getLeaderName() != null && !auction.getLeaderName().isEmpty())
                            ? auction.getLeaderName()
                            : "Chưa có ai đặt giá";

                    // Nếu chưa có ai đặt thì in giá khởi điểm, có rồi thì in giá hiện tại
                    double displayPrice = auction.getCurrentPrice() > 0 ? auction.getCurrentPrice() : product.getStartPrice();

                    lblTopBidder.setText(leader + " - " + String.format("%,.0f VNĐ", displayPrice));
                }

                if (productImage != null) {
                    String imageSource = product.getImagePath();

                    // Kiểm tra xem link có tồn tại không
                    if (imageSource != null && !imageSource.isEmpty() && imageSource.startsWith("http")) {
                        try {
                            javafx.scene.image.Image img = new javafx.scene.image.Image(imageSource, true);
                            productImage.setImage(img);
                        } catch (Exception e) {
                            System.out.println("[TikTok UI] Không thể load ảnh từ Cloudinary: " + e.getMessage());
                            productImage.setImage(null);
                        }
                    } else {
                        productImage.setImage(null);
                    }
                }
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
            RequestSender.sendGetActiveAuctionsRequest();
        }
    }

    public void cleanup() {
        ControllerRegistry.unregister("TikTokAuctionController");
        RequestSender.send(MessageType.STOP_TIK_TOK_LISTENER_REQUEST, new java.util.HashMap<>());
    }

    @FXML
    public void handleBidding(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/biddingBoard.fxml");
    }

    @FXML
    public void handleBotBid(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/botBidding.fxml");
    }

    // THÊM HÀM NÀY ĐỂ XỬ LÝ NHẢY SỐ REAL-TIME TỪ SERVER GỬI VỀ

    public void updateRealtimeBid(String productId, double newPrice, String leaderName) {
        Platform.runLater(() -> {
            // Lấy món đồ đang được hiển thị trên màn hình hiện tại
            Auction currentAuction = ClientContext.getInstance().getCurrentAuction();

            // Nếu ID của món đồ trên màn hình TRÙNG với ID của món vừa được trả giá
            if (currentAuction != null && currentAuction.getProduct() != null
                    && currentAuction.getProduct().getId().equals(productId)) {
                // 1. Nhảy số tiền trực tiếp trên UI
                price.setText(String.format("%,.0f VNĐ", newPrice));

                // 2. Cập nhật luôn giá trị vào RAM (để khi người dùng lướt Up/Down quay lại vẫn giữ giá mới)
                currentAuction.setCurrentPrice(newPrice);
                if (lblTopBidder != null) {
                    lblTopBidder.setText(leaderName + " - " + String.format("%,.0f VNĐ", newPrice));
                }
                System.out.println("[TikTok UI] Đã nhảy số trực tiếp trên màn hình: " + newPrice);
            }
        });
    }
}