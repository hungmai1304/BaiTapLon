package com.auction.client.handler.bidding;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.tiktok.BiddingController;
import com.auction.client.controller.tiktok.TikTokAuctionController;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import javafx.application.Platform;

import java.util.logging.Logger;

@ResponseHandler(type = MessageType.BROADCAST_NEW_BID)
public class NewBidClientHandler implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(NewBidClientHandler.class.getName());

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    // 1. Trích xuất dữ liệu từ Response của gói tin Real-time
                    double newPrice = ((Number) response.getData().get("newPrice")).doubleValue();
                    String leaderName = (String) response.getData().get("leaderName");
                    String productId = (String) response.getData().get("productId");

                    LOGGER.info("[Client] Nhận giá mới: " + newPrice + " từ " + leaderName);

                    // In log kiểm tra trạng thái gói tin nhận được
                    System.out.println("[Client] Nhận giá mới: " + newPrice + " từ " + leaderName
                            + (newEndTime != null ? " [GIA HẠN ANTI-SNIPING: " + newEndTime + "]" : ""));

                    // 2. GỌI CONTROLLER QUA REGISTRY ĐỂ CẬP NHẬT UI REAL-TIME

                    // Cập nhật cho BiddingController nếu đang mở bảng đặt giá
                    BiddingController biddingCtrl = (BiddingController) ControllerRegistry.get("BiddingController");
                    if (biddingCtrl != null) {
                        biddingCtrl.updateRealtimeBid(productId, newPrice, leaderName, newEndTime);
                    }

                    // Cập nhật cho TikTokAuctionController nếu đang xem luồng video/sản phẩm phong cách TikTok
                    TikTokAuctionController controller = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");
                    if (controller != null) {
                        // Truyền đầy đủ dữ liệu (bao gồm cả newEndTime) sang UI để tự động nhảy số tiền và cập nhật bộ đếm giờ
                        controller.updateRealtimeBid(productId, newPrice, leaderName, newEndTime);
                    } else {
                        LOGGER.info("[Client] Giao diện TikTok chưa mở, không cần nhảy số.");
                    }

                } catch (Exception e) {
                    System.err.println("[NewBidClientHandler] Lỗi xử lý gói tin BROADCAST_NEW_BID: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}