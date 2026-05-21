package com.auction.client.handler.bidding;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.mainHome.WonAuctionsController;
import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.util.List;

@ResponseHandler(type = MessageType.GET_WON_AUCTIONS_RESPONSE)
public class GetWonAuctionsResponseHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equals(response.getStatus())) {
            // Dữ liệu list Auction nằm trong map 'data' của Response với key 'wonAuctions'
            Object rawData = response.getData().get("wonAuctions");
            String json = ClientMessageDispatcher.gson.toJson(rawData);
            
            List<Auction> wonAuctions = ClientMessageDispatcher.gson.fromJson(json, new TypeToken<List<Auction>>(){}.getType());
            
            Platform.runLater(() -> {
                WonAuctionsController controller = (WonAuctionsController) ControllerRegistry.get("WonAuctionsController");
                if (controller != null) {
                    controller.updateTable(wonAuctions);
                }
            });
        }
    }
}
