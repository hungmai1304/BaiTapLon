package com.auction.client.network;

import com.auction.common.model.product.Product;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

public class RequestSender {
    // Khởi tạo một đối tượng Gson dùng chung để tiết kiệm tài nguyên
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) -> {
                // Biến LocalDateTime thành chuỗi String để gửi đi không bị lỗi
                return new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            })
            .create();

    private RequestSender() {
    }

    /**
     * Hàm phụ trợ để đóng gói và gửi request nhanh
     */
    private static void send(String type, Object data) {
        Map<String, Object> requestMap = new HashMap<>();
        requestMap.put("type", type);
        requestMap.put("data", data);

        // Gson tự động handle việc bọc ngoặc kép, ký tự đặc biệt và Base64
        String json = gson.toJson(requestMap);

        // Gửi đi, không in ra Console nữa
        NetworkClient.sendCommand(json);
    }

    // =====================================================
    // LOGIN
    // =====================================================
    public static void sendLoginRequest(String email, String password) {
        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);
        send("LOGIN_REQUEST", data);
    }

    // =====================================================
    // REGISTER
    // =====================================================
    public static void sendRegisterRequest(String name, String email, String password, String role, String shopName) {
        Map<String, String> data = new HashMap<>();
        data.put("name", name);
        data.put("email", email);
        data.put("password", password);
        data.put("role", role);
        data.put("shopName", shopName != null ? shopName : "");
        send("REGISTER_REQUEST", data);
    }

    // =====================================================
    // IMPORT PRODUCT (SỬ DỤNG BASE64 CHUẨN)
    // =====================================================
    public static void sendImportProductRequest(Product product) {
        // Gửi nguyên đối tượng Product, Gson sẽ tự lấy các field id, name, imageBase64...
        send("IMPORT_PRODUCT_REQUEST", product);
    }

    // =====================================================
    // EDIT PRODUCT
    // =====================================================
    public static void sendEditProductRequest(Product product) {
        send("EDIT_PRODUCT_REQUEST", product);
    }

    // =====================================================
    // GET SHOP PRODUCTS
    // =====================================================
    public static void sendGetShopProductsRequest(String ownerId) {
        Map<String, String> data = new HashMap<>();
        data.put("sellerId", ownerId);
        send("GET_SHOP_PRODUCTS_REQUEST", data);
    }

    // =====================================================
    // SELL PRODUCT (BẮT ĐẦU ĐẤU GIÁ)
    // =====================================================
    public static void sendSellProductRequest(String productId) {
        Map<String, String> data = new HashMap<>();
        data.put("id", productId);
        send("SELL_PRODUCT_REQUEST", data);
    }
    public static void sendPlaceBidRequest(String productId, double bidAmount, String userEmail) {
        String json = "{"
                + "\"type\":\"PLACE_BID_REQUEST\","
                + "\"data\":{"
                + "\"productId\":\"" + productId + "\","
                + "\"bidAmount\":" + bidAmount + ","
                + "\"email\":\"" + userEmail + "\""
                + "}"
                + "}";
        NetworkClient.sendCommand(json);
    }

    public static void sendRegisterBotRequest(String productId, double maxPrice, double botStep, String userEmail) {
        String json = "{"
                + "\"type\":\"REGISTER_BOT_REQUEST\","
                + "\"data\":{"
                + "\"productId\":\"" + productId + "\","
                + "\"maxPrice\":" + maxPrice + ","
                + "\"botStep\":" + botStep + ","
                + "\"email\":\"" + userEmail + "\""
                + "}"
                + "}";
        NetworkClient.sendCommand(json);
    }
}