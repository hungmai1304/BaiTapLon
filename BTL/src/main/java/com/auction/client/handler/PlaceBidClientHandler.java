package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.controller.BiddingController;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;

@ResponseHandler(type = MessageType.PLACE_BID_RESPONSE)
public class PlaceBidClientHandler implements IClientHandler {
    @Override
    public void handle(Response response) {
        BiddingController controller = (BiddingController) ControllerRegistry.get("BiddingController");
        if (controller != null) {
            boolean isSuccess = "SUCCESS".equals(response.getStatus());
            controller.showServerNotification(response.getMessage(), isSuccess);
        }
    }
}