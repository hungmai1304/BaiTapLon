package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.auction.Auction; // Đã đổi sang Import Auction
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.util.List;

@ResponseHandler(type = MessageType.GET_ACTIVE_AUCTIONS_RESPONSE)
public class GetAuctionProductClientHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        try {
            // Lấy danh sách PHIÊN ĐẤU GIÁ (Dặn Server trả về key là "auctions")
            Object rawData = response.getData().get("auctions");

            List<Auction> auctions = ClientMessageDispatcher.gson.fromJson(
                    ClientMessageDispatcher.gson.toJson(rawData),
                    new TypeToken<List<Auction>>(){}.getType()
            );

            if (auctions != null && !auctions.isEmpty()) {
                // Cập nhật vào Context
                ClientContext.getInstance().setAuctionList(auctions);

                Platform.runLater(() -> {
                    TikTokAuctionController controller = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");

                    if (controller != null) {
                        Auction currentAuction = ClientContext.getInstance().getCurrentAuction();
                        if (currentAuction != null) {
                            controller.updateUI(currentAuction);
                        }
                    }
                });

                System.out.println("-> [Handler] Nhận " + auctions.size() + " phiên đấu giá từ Server.");
            } else {
                System.out.println("-> [Handler] Hiện tại không có phiên đấu giá nào.");
            }

        } catch (Exception e) {
            System.err.println("-> [Handler Error] Lỗi khi xử lý danh sách đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }
}