package com.auction.client.handler.bidding;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.tiktok.BiddingController;
import com.auction.client.controller.tiktok.TikTokAuctionController;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import javafx.application.Platform;

/**
 * Handler xử lý sự kiện Broadcast giá mới từ Server gửi về cho Client.
 * Đã gộp hoàn chỉnh cơ chế đồng bộ biến thời gian kết thúc mới (newEndTime) phục vụ Anti-Sniping.
 */
@ResponseHandler(type = MessageType.BROADCAST_NEW_BID)
public class NewBidClientHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    // 1. Trích xuất dữ liệu từ Response của gói tin Real-time
                    double newPrice = ((Number) response.getData().get("newPrice")).doubleValue();
                    String leaderName = (String) response.getData().get("leaderName");
                    String productId = (String) response.getData().get("productId");

                    // Lấy biến gia hạn thời gian kết thúc (Anti-Sniping) từ File 1
                    String newEndTime = (String) response.getData().get("newEndTime");

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
                        System.out.println("[Client] Giao diện TikTok chưa mở, không cần nhảy số.");
                    }

                } catch (Exception e) {
                    System.err.println("[NewBidClientHandler] Lỗi xử lý gói tin BROADCAST_NEW_BID: " + e.getMessage());
                    e.printStackTrace();
                }
            });
        }
    }
}