package com.auction.client.network;

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
}