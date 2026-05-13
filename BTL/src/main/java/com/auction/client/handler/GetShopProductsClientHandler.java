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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.auction.client.controller.ShopSellController;
import com.auction.client.controller.MainController; // Import MainController
import com.auction.client.controller.SomeGlobal;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;

@ResponseHandler(type = "GET_SHOP_PRODUCTS_RESPONSE")
public class GetShopProductsClientHandler implements IClientHandler {

    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class,
                    (JsonDeserializer<LocalDateTime>) (json, type, context) ->
                            LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {

            Platform.runLater(() -> {
                System.out.println("-> [GetShopProductsClientHandler] Nhận danh sách sản phẩm shop!");

                try {
                    // 1. Parse dữ liệu từ Response
                    String jsonData = gson.toJson(response.getData().get("products"));
                    Type listType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> shopProducts = gson.fromJson(jsonData, listType);

                    if (shopProducts != null) {
                        System.out.println("-> Số sản phẩm nhận về: " + shopProducts.size());

                        // 2. CẬP NHẬT SỐ LƯỢNG CHO MÀN HÌNH MAIN
                        // Đây là đoạn bố cần để hiện số lên Main
                        MainController mainController = SomeGlobal.getMainController();
                        if (mainController != null) {
                            mainController.updateProductCount(shopProducts.size());
                            System.out.println("-> Đã cập nhật số lượng lên màn hình Main.");
                        }

                        // 3. CẬP NHẬT CHO MÀN HÌNH SHOP (Nếu đang mở)
                        ShopSellController shopController = SomeGlobal.getShopSellController();
                        if (shopController != null) {
                            shopController.loadProducts(shopProducts);
                        }
                    }

                } catch (Exception e) {
                    System.err.println("-> Lỗi phân tích sản phẩm shop: " + e.getMessage());
                    e.printStackTrace();
                }
            });

        } else {
            System.err.println("-> Lỗi từ Server: " + response.getMessage());
        }
    }
}