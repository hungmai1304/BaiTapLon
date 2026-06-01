package com.auction.client.handler.bidding;

import com.auction.client.controller.tiktok.BiddingController;
import com.auction.client.utils.ControllerRegistry;
import com.auction.protocol.Response;
import com.auction.client.network.IClientHandler;
import java.util.Map;
import java.util.logging.Logger;

public class BroadcastBidHandler implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(BroadcastBidHandler.class.getName());
    @Override
    public void handle(Response response) {
        try {
            // --- BƯỚC QUAN TRỌNG: Bóc dữ liệu từ Server gửi về ---
            Map<String, Object> data = (Map<String, Object>) response.getData();

            // Lấy giá mới và tên người dẫn đầu từ gói tin
            double newPrice = Double.parseDouble(data.get("newPrice").toString());
            String leaderName = data.get("leaderName").toString();

            // Tìm màn hình Bidding đang mở để ép số nhảy
            Object controller = ControllerRegistry.get("BiddingController");

            if (controller instanceof BiddingController) {
                BiddingController biddingController = (BiddingController) controller;

                // Gọi hàm xịn mà anh em mình vừa code bên BiddingController
                biddingController.updateAuctionPriceRealtime(newPrice, leaderName);

                LOGGER.info("🔹 [Broadcast] Đã ép nhảy số trên màn hình lên: " + newPrice);
            } else {
                LOGGER.info("🔹 [Broadcast] Nhận giá mới nhưng người dùng không ở màn Bidding.");
            }

        } catch (Exception e) {
            LOGGER.severe("Lỗi khi bóc tách Broadcast: " + e.getMessage());
        }
    }
}