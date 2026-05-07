package com.auction.client.network;

import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

public class RequestSender {

    private RequestSender() {
    }

    // =====================================================
    // LOGIN
    // =====================================================
    private static final Gson gson = new Gson();

    public static void sendLoginRequest(String email, String password) {
        Map<String, Object> request = new HashMap<>();
        request.put("type", "LOGIN_REQUEST");

        Map<String, String> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);
        request.put("data", data);

        NetworkClient.sendCommand(gson.toJson(request)); // An toàn hơn nhiều
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
}