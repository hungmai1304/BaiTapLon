package com.auction.client.network;

import com.auction.common.model.product.Product;

// Nơi nhận tin nhắn gửi đến -> đóng gói -> và gửi
public class RequestSender {

    private RequestSender() {
    }

    // =====================================================
    // LOGIN
    // =====================================================
    public static void sendLoginRequest(
            String email,
            String password
    ) {

        String json = "{"
                + "\"type\":\"LOGIN_REQUEST\","
                + "\"data\":{"
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(json);
    }

    // =====================================================
    // REGISTER
    // =====================================================
    public static void sendRegisterRequest(
            String name,
            String email,
            String password,
            String role,
            String shopName // 1. Thêm tham số shopName
    ) {
        // 2. Bổ sung field "shopName" vào trong object "data"
        // Lưu ý dấu phẩy sau trường "role"
        String json = "{"
                + "\"type\":\"REGISTER_REQUEST\","
                + "\"data\":{"
                + "\"name\":\"" + name + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\","
                + "\"role\":\"" + role + "\","
                + "\"shopName\":\"" + (shopName != null ? shopName : "") + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(json);
    }
    //=======================================================
    // IMPORT PRODUCT
    //=======================================================
    public static void sendImportProductRequest(Product product) {
        String json = "{"
                + "\"type\":\"IMPORT_PRODUCT_REQUEST\","
                + "\"data\":{"
                + "\"id\":\"" + product.getId() + "\","
                + "\"timeCreate\":\"" + product.getTimeCreated() + "\","
                + "\"name\":\"" + product.getName() + "\","
                + "\"category\":\"" + product.getCategory() + "\","
                + "\"startPrice\":" + product.getStartPrice() + ","
                + "\"currentPrice\":" + product.getCurrentPrice() + ","
                + "\"stepPrice\":" + product.getStepPrice() + ","
                + "\"owner\":\"" + product.getOwner() + "\","
                + "\"status\":\"" + product.getStatus() + "\"," // Nhớ dấu phẩy ở đây
                + "\"description\":\"" + (product.getDescription() != null ? product.getDescription() : "") + "\","
                + "\"imageBase64\":\"" + (product.getImageBase64() != null ? product.getImageBase64() : "") + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(json);
    }

    // load sản phẩm của user
    public static void sendGetShopProductsRequest(String ownerId) {
        String json = "{"
                + "\"type\":\"GET_SHOP_PRODUCTS_REQUEST\","
                + "\"data\":{"
                + "\"sellerId\":\"" + ownerId + "\""
                + "}"
                + "}";
        NetworkClient.sendCommand(json);
    }
    public static void sendEditProductRequest(Product product) {
        /*
         * Cấu trúc JSON gửi đi bao gồm đầy đủ các thông tin của Product:
         * id, timeCreate, name, category, startPrice, currentPrice, stepPrice, owner, status và description.
         */

        String json = "{"
                + "\"type\":\"EDIT_PRODUCT_REQUEST\","
                + "\"data\":{"
                // Dữ liệu định danh và thời gian
                + "\"id\":\"" + product.getId() + "\","
                + "\"timeCreate\":\"" + product.getTimeCreated() + "\","

                // Thông tin cơ bản
                + "\"name\":\"" + product.getName() + "\","
                + "\"category\":\"" + product.getCategory() + "\","

                // Thông tin giá cả (Dạng số không cần dấu ngoặc kép)
                + "\"startPrice\":" + product.getStartPrice() + ","
                + "\"currentPrice\":" + product.getCurrentPrice() + ","
                + "\"stepPrice\":" + product.getStepPrice() + ","

                // Thông tin sở hữu và trạng thái
                + "\"owner\":\"" + product.getOwner() + "\","
                + "\"status\":\"" + product.getStatus() + "\","

                // Mô tả sản phẩm (Xử lý null để tránh lỗi chuỗi "null")
                + "\"description\":\"" + (product.getDescription() != null ? product.getDescription() : "") + "\","
                + "\"imageBase64\":\"" + (product.getImageBase64() != null ? product.getImageBase64() : "") + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(json);
    }
    public static void sendSellProductRequest(String productId) {
        // Nối chuỗi JSON bằng tay giống y hệt các hàm ở trên
        String json = "{"
                + "\"type\":\"SELL_PRODUCT_REQUEST\","
                + "\"data\":{"
                + "\"id\":\"" + productId + "\""
                + "}"
                + "}";

        // Gọi lệnh tĩnh giống hệ thống cũ
        NetworkClient.sendCommand(json);
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