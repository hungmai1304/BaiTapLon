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
            try {
                // 1. Parse dữ liệu
                Object productsObj = response.getData().get("products");
                String jsonData = gson.toJson(productsObj);
                Type listType = new TypeToken<List<Product>>(){}.getType();
                List<Product> shopProducts = gson.fromJson(jsonData, listType);

                if (shopProducts != null) {
                    // 2. Lưu vào ClientContext
                    ClientContext.getInstance().setShopProducts(shopProducts);

                    // 3. Cập nhật UI
                    Platform.runLater(() -> {
                        // Cập nhật số lượng hiển thị trên Main
                        MainController main = SomeGlobal.getMainController();
                        if (main != null) {
                            main.updateProductCount(ClientContext.getInstance().getShopProductCount());
                        }

                        // Yêu cầu ShopController render từ danh sách trong Context
                        ShopSellController shop = SomeGlobal.getShopSellController();
                        if (shop != null) {
                            shop.loadProducts(ClientContext.getInstance().getShopProducts());
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}