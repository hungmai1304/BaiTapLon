package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.auction.Auction; // Đổi từ Product sang Auction
import com.auction.protocol.Response;
import com.google.gson.Gson;
import javafx.application.Platform;

@ResponseHandler(type = "PRODUCT_RESPONSE")
public class ProductResponseHandler implements IClientHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            TikTokAuctionController controller = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");

            if (controller != null) {
                try {
                    // 1. Parse dữ liệu từ Server về đối tượng Auction (Phiên đấu giá)
                    String jsonData = gson.toJson(response.getData());
                    Auction auction = gson.fromJson(jsonData, Auction.class);

                    // 2. Cập nhật UI thông qua Controller (Dùng Platform.runLater đã có trong updateUI)
                    if (auction != null) {
                        controller.updateUI(auction);
                        System.out.println("-> [Handler] Đã cập nhật thông tin phiên đấu giá: " + auction.getId());
                    }
                } catch (Exception e) {
                    System.err.println("-> [Handler Error] Lỗi parse dữ liệu Auction: " + e.getMessage());
                }
            }
        } else {
            System.err.println("❌ Lỗi từ Server: " + response.getMessage());
        }
    }
}