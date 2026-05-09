package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
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

            Platform.runLater(() -> {
                System.out.println("✅ [GetAuctionProductClientHandler] Nhận sản phẩm đang đấu giá từ Server!");

                try {
                    // Lấy danh sách sản phẩm từ response.data.products
                    String jsonData = gson.toJson(response.getData().get("products"));
                    Type listType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> auctionProducts = gson.fromJson(jsonData, listType);

                    System.out.println("🔴 Số sản phẩm đang đấu giá: " + auctionProducts.size());
                    for (Product p : auctionProducts) {
                        System.out.println("  📍 " + p.getName() +
                                " | Status: " + p.getStatus() +
                                " | Giá: " + p.getCurrentPrice());
                    }

                    // 👉 TODO: Gọi UI để hiển thị
                    // Ví dụ: SomeGlobal.getMainViewController().displayAuctionProducts(auctionProducts);

                } catch (Exception e) {
                    System.err.println("❌ Lỗi phân tích sản phẩm đấu giá: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } else {
            System.err.println("❌ Lỗi từ Server: " + response.getMessage());
        }
    }
}