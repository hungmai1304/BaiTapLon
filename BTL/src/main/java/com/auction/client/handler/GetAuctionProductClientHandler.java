package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.auction.Auction;
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
            // Lấy dữ liệu từ Server với key "auctionList"
            Object rawData = response.getData().get("auctionList");

            if (rawData == null) return;

            // Ép kiểu JSON sang List<Auction>
            List<Auction> auctions = ClientMessageDispatcher.gson.fromJson(
                    ClientMessageDispatcher.gson.toJson(rawData),
                    new TypeToken<List<Auction>>(){}.getType()
            );

            if (auctions != null && !auctions.isEmpty()) {
                // Cập nhật vào danh sách quản lý duy nhất trong ClientContext
                ClientContext.getInstance().setAuctionList(auctions);

                Platform.runLater(() -> {
                    // Lấy Controller từ Registry để cập nhật giao diện
                    TikTokAuctionController controller = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");
                    if (controller != null) {
                        // Render phiên đấu giá tại vị trí con trỏ hiện tại
                        controller.renderCurrentAuction();
                    }
                });

                System.out.println("-> [Handler] Đã cập nhật " + auctions.size() + " phiên đấu giá vào danh sách mới.");
            }
        } catch (Exception e) {
            System.err.println("-> [Handler Error] Lỗi cập nhật danh sách đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }
}