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
            String role
    ) {

        String json = "{"
                + "\"type\":\"REGISTER_REQUEST\","
                + "\"data\":{"
                + "\"name\":\"" + name + "\","
                + "\"email\":\"" + email + "\","
                + "\"password\":\"" + password + "\","
                + "\"role\":\"" + role + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(json);
    }
    //=======================================================
    // IMPORT PRODUCT
    //=======================================================
    public static void sendImportProductRequest(Product product) {
        /*
         * Cấu trúc JSON dựa trên sơ đồ:
         * entity: id, timeCreate
         * item: name (kế thừa entity)
         * product: category, startPrice, currentPrice, stepPrice, owner, status (kế thừa item)
         */

        String json = "{"
                + "\"type\":\"IMPORT_PRODUCT_REQUEST\","
                + "\"data\":{"
                // Dữ liệu từ Entity
                + "\"id\":\"" + product.getId() + "\","
                + "\"timeCreate\":\"" + product.getTimeCreated() + "\","

                // Dữ liệu từ Item
                + "\"name\":\"" + product.getName() + "\","

                // Dữ liệu từ Product
                + "\"category\":\"" + product.getCategory() + "\","
                + "\"startPrice\":" + product.getStartPrice() + ","
                + "\"currentPrice\":" + product.getCurrentPrice() + ","
                + "\"stepPrice\":" + product.getStepPrice() + ","
                + "\"owner\":\"" + product.getOwner() + "\","
                + "\"status\":\"" + product.getStatus() + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(json);
    }
}