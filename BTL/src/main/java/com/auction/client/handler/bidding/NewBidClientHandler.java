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
                    double newPrice = ((Number) response.getData().get("newPrice")).doubleValue();
                    String leaderName = (String) response.getData().get("leaderName");
                    String productId = (String) response.getData().get("productId");

                    LOGGER.info("[Client] Nhận giá mới: " + newPrice + " từ " + leaderName);

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
                        LOGGER.info("[Client] Giao diện TikTok chưa mở, không cần nhảy số.");
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}