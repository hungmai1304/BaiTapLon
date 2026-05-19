package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.BiddingController;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import javafx.application.Platform;

@ResponseHandler(type = MessageType.BROADCAST_NEW_BID)
public class NewBidClientHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    double newPrice = ((Number) response.getData().get("newPrice")).doubleValue();
                    String leaderName = (String) response.getData().get("leaderName");
                    String productId = (String) response.getData().get("productId");

                    System.out.println("[Client] Nhận giá mới: " + newPrice + " từ " + leaderName);

                    // GỌI CONTROLLER QUA REGISTRY ĐỂ NHẢY SỐ UI

                    TikTokAuctionController controller = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");
                    BiddingController biddingCtrl = (BiddingController) ControllerRegistry.get("BiddingController");
                    if (biddingCtrl != null) {
                        biddingCtrl.updateRealtimeBid(productId, newPrice, leaderName);
                    }

                    if (controller != null) {
                        // Truyền dữ liệu sang UI để nó tự nhảy số
                        controller.updateRealtimeBid(productId, newPrice, leaderName);
                    } else {
                        System.out.println("[Client] Giao diện TikTok chưa mở, không cần nhảy số.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}