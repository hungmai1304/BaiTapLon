package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.util.List;

@ResponseHandler(type = "GET_SHOP_PRODUCTS_RESPONSE")
public class GetShopProductsClientHandler implements IClientHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {

            Platform.runLater(() -> {
                System.out.println("✅ [GetShopProductsClientHandler] Nhận danh sách sản phẩm shop!");

                try {
                    // Lấy danh sách từ response.data.products
                    String jsonData = gson.toJson(response.getData().get("products"));
                    Type listType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> shopProducts = gson.fromJson(jsonData, listType);

                    System.out.println("🏪 Số sản phẩm trong shop: " + shopProducts.size());

                    // Phân loại theo trạng thái
                    long availableCount = shopProducts.stream()
                            .filter(p -> p.getStatus() == ProductStatus.AVAILABLE)
                            .count();
                    long onAuctionCount = shopProducts.stream()
                            .filter(p -> p.getStatus() == ProductStatus.ON_AUCTION)
                            .count();
                    long soldCount = shopProducts.stream()
                            .filter(p -> p.getStatus() == ProductStatus.SOLD)
                            .count();

                    System.out.println("  ⏳ AVAILABLE: " + availableCount);
                    System.out.println("  🔴 ON_AUCTION: " + onAuctionCount);
                    System.out.println("  ✅ SOLD: " + soldCount);

                    for (Product p : shopProducts) {
                        System.out.println("  📦 " + p.getName() +
                                " | Status: " + p.getStatus() +
                                " | Giá: " + p.getCurrentPrice());
                    }

                    // 👉 TODO: Gọi UI để hiển thị
                    // Ví dụ: SomeGlobal.getShopController().displayShopProducts(shopProducts);

                } catch (Exception e) {
                    System.err.println("❌ Lỗi phân tích sản phẩm shop: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } else {
            System.err.println("❌ Lỗi từ Server: " + response.getMessage());
        }
    }
}