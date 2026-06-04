package com.auction.client.handler.shop;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.NavigationService;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import javafx.application.Platform;

import java.util.logging.Logger;

@ResponseHandler(type = MessageType.DELETE_PRODUCT_RESPONSE)
public class DeleteProductClientHandler implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(DeleteProductClientHandler.class.getName());
    @Override
    public void handle(Response response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    NavigationService.setCenterView("/com/auction/client/view/ShopSell.fxml");
                    RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}