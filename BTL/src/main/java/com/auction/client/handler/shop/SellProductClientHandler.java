package com.auction.client.handler.shop;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.network.RequestSender;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import javafx.application.Platform;

import java.util.logging.Logger;

@ResponseHandler(type = "SELL_PRODUCT_RESPONSE")
public class SellProductClientHandler implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(SellProductClientHandler.class.getName());

    @Override
    public void handle(Response response) {
        Platform.runLater(() -> {
            if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
                LOGGER.info("[Client] Đã đưa sản phẩm lên sàn thành công!");
                LOGGER.info("   Message: " + response.getMessage());

                // TODO: Refresh lại UI - reload danh sách shop
                RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);

            } else {
                LOGGER.severe("[Client] Lỗi khi lên sàn: " + response.getMessage());
            }
        });
    }
}