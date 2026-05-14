package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.auction.client.controller.ShopSellController;
import com.auction.client.controller.MainController;
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
                System.out.println("-> [GetShopProductsClientHandler] Nhận danh sách sản phẩm từ Cloudinary!");

                try {
                    // 1. Parse dữ liệu từ Response
                    // Lúc này trong mỗi Product, imagePath đã là link "https://res.cloudinary.com/..."
                    Object productsObj = response.getData().get("products");
                    String jsonData = gson.toJson(productsObj);
                    Type listType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> shopProducts = gson.fromJson(jsonData, listType);

                    if (shopProducts != null) {
                        System.out.println("-> Số sản phẩm nhận về: " + shopProducts.size());

                        // 2. CẬP NHẬT SỐ LƯỢNG CHO MÀN HÌNH MAIN
                        MainController mainController = SomeGlobal.getMainController();
                        if (mainController != null) {
                            mainController.updateProductCount(shopProducts.size());
                        }

                        // 3. CẬP NHẬT CHO MÀN HÌNH SHOP (Nạp danh sách vào UI)
                        // Các item trong Shop sẽ dùng link URL để load ảnh cực nhanh
                        ShopSellController shopController = SomeGlobal.getShopSellController();
                        if (shopController != null) {
                            shopController.loadProducts(shopProducts);
                        }

                        System.out.println("-> [Client] Đã hiển thị danh sách sản phẩm thành công.");
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