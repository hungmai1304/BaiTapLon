package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.google.gson.Gson;

@ResponseHandler(type = "PRODUCT_RESPONSE")
public class ProductResponseHandler implements IClientHandler {
    private final Gson gson = new Gson();

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            TikTokAuctionController controller = ControllerRegistry.get("TikTokAuctionController");
            if (controller != null) {
                // Parse object Product từ data của response
                String jsonData = gson.toJson(response.getData());
                Product product = gson.fromJson(jsonData, Product.class);

                controller.updateUI(product);
            }
        } else {
            System.err.println("❌ Lỗi từ Server: " + response.getMessage());
        }
    }
}