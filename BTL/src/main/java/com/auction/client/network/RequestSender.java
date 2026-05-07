package com.auction.client.network;

public class RequestSender {

    private RequestSender() {
    }

    // LOGIN
    public static void sendLoginRequest(String email,
                                        String password) {

        String jsonRequest = "{"
                + "\"type\":\"LOGIN_REQUEST\","
                + "\"data\":{"
                + "\"username\":\"" + email + "\","
                + "\"password\":\"" + password + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(jsonRequest);
    }

    // REGISTER
    public static void sendRegisterRequest(String email,
                                           String password) {

        String jsonRequest = "{"
                + "\"type\":\"REGISTER_REQUEST\","
                + "\"data\":{"
                + "\"username\":\"" + email + "\","
                + "\"password\":\"" + password + "\""
                + "}"
                + "}";

        NetworkClient.sendCommand(jsonRequest);
    }

    // BID
    public static void sendBidRequest(double amount) {

        String jsonRequest = "{"
                + "\"type\":\"BID_REQUEST\","
                + "\"data\":{"
                + "\"amount\":" + amount
                + "}"
                + "}";

        NetworkClient.sendCommand(jsonRequest);
    }
}