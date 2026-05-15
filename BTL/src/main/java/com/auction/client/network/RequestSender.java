package com.auction.client.network;

import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class RequestSender {
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .create();

    private RequestSender() {}

    public static void send(String type, Object data) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("type", type);
        requestMap.put("data", data);
        String json = gson.toJson(requestMap);
        NetworkClient.sendCommand(json);
    }

    public static void sendLoginRequest(String email, String password) {
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);
        send(MessageType.LOGIN_REQUEST, data);
    }

    public static void sendRegisterRequest(String name, String email, String password, String role, String shopName) {
        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("password", password);
        data.put("role", role);
        data.put("shopName", shopName != null ? shopName : "");
        send(MessageType.REGISTER_REQUEST, data);
    }

    public static void sendImportProductRequest(Product product) {
        send(MessageType.IMPORT_PRODUCT_REQUEST, product);
    }

    public static void sendEditProductRequest(Product product) {
        send(MessageType.EDIT_PRODUCT_REQUEST, product);
    }

    public static void sendGetShopProductsRequest(String ownerId) {
        Map<String, String> data = new HashMap<>();
        data.put("sellerId", ownerId);
        send(MessageType.GET_SHOP_PRODUCTS_REQUEST, data);
    }

    // --- ĐẤU GIÁ (AUCTIONS) ---

    public static void sendGetActiveAuctionsRequest() {
        send(MessageType.GET_ACTIVE_AUCTIONS_REQUEST, new HashMap<>());
    }

    // ĐÃ SỬA: Quay về nhận productId kiểu String
    public static void sendSellProductRequest(String productId) {
        Map<String, String> data = new HashMap<>();
        data.put("id", productId);
        send(MessageType.SELL_PRODUCT_REQUEST, data);
    }

    public static void sendPlaceBidRequest(String productId, double bidAmount, String userEmail) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("bidAmount", bidAmount);
        data.put("email", userEmail);
        send(MessageType.PLACE_BID_REQUEST, data);
    }

    public static void sendRegisterBotRequest(String productId, double maxPrice, double botStep, String userEmail) {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", productId);
        data.put("maxPrice", maxPrice);
        data.put("botStep", botStep);
        data.put("email", userEmail);
        send("REGISTER_BOT_REQUEST", data);
    }
}