package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.util.List;

@ResponseHandler(type = "GET_AUCTION_PRODUCT_RESPONSE")
public class GetAuctionProductClientHandler implements IClientHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            try {
                // 1. Phân tích dữ liệu JSON
                String jsonData = gson.toJson(response.getData().get("products"));
                Type listType = new TypeToken<List<Product>>(){}.getType();
                List<Product> auctionProducts = gson.fromJson(jsonData, listType);

                // 2. Cập nhật vào ClientContext (Chạy trên UI Thread của JavaFX)
                Platform.runLater(() -> {
                    ClientContext.getInstance().setAuctionProducts(auctionProducts);
                    System.out.println("✅ Đã cập nhật " + auctionProducts.size() + " sản phẩm vào ClientContext");
                });

            } catch (Exception e) {
                System.err.println("❌ Lỗi phân tích dữ liệu: " + e.getMessage());
            }
        }
    }
}