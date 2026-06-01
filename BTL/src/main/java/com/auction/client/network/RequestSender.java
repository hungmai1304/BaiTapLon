package com.auction.client.network;

import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class RequestSender {
    private static final Logger LOGGER = Logger.getLogger(RequestSender.class.getName());
    // NÂNG CẤP GSON: Đăng ký cả bộ Serializer để gửi đi mượt mà không lỗi chuỗi định dạng
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new com.google.gson.JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    private RequestSender() {}

    public static void send(String type, Object data) {
        try {
            Map<String, Object> requestMap = new HashMap<>();
            requestMap.put("type", type);
            requestMap.put("data", data);

            String json = gson.toJson(requestMap);
            LOGGER.info("[RequestSender] JSON Gửi đi: " + json); // Log ra để dễ debug theo dõi
            NetworkClient.sendCommand(json);
        } catch (Exception e) {
            LOGGER.severe("[RequestSender Error] Lỗi khi tạo JSON string: " + e.getMessage());
            e.printStackTrace();
        }
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

    // --- ĐẤU GIÁ (AUCTIONS) ---

    public static void sendGetActiveAuctionsRequest() {
        send(MessageType.GET_ACTIVE_AUCTIONS_REQUEST, new HashMap<>());
    }

    /**
     * SỬA LẠI CHUẨN: Hàm này nhận Map dữ liệu đầy đủ (bao gồm cả id, startPrice, thời gian)
     * từ ShopSellController để đẩy thẳng lên Server SellProductHandler.
     */
    public static void sendSellProductRequest(Map<String, Object> sellData) {
        send(MessageType.SELL_PRODUCT_REQUEST, sellData);
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
        send(MessageType.REGISTER_BOT_REQUEST, data);
    }

    // đừng có xóa của tao:
    // RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);

    public static void sendDeleteProductRequest(String productId) {
        Map<String, String> data = new HashMap<>();
        data.put("id", productId);
        send(MessageType.DELETE_PRODUCT_REQUEST, data);
    }
}