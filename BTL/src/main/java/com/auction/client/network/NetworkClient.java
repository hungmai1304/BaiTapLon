package com.auction.client.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class NetworkClient {

    // Production
//    private static final String SERVER_URL ="ws://f95392e43d88892f-58-186-123-109.serveousercontent.com";
//    private static final String SERVER_URL ="wss://baitaplon-qegw.onrender.com";
    private static final String SERVER_URL = "ws://localhost:10000";


    // Local
    //private static final String SERVER_URL ="ws://localhost:10000";

    private static WebSocketClient webSocketClient;
    private static MessageListener currentListener;

    private NetworkClient() {
    }

    public static void connectAndKeepAlive() {

        if (webSocketClient != null
                && webSocketClient.isOpen()) {

            System.out.println("? ?� ???c k?t n?i t?i server.");

            return;
        }

        try {

            webSocketClient = new WebSocketClient(
                    new URI(SERVER_URL)
            ) {

                @Override
                public void onOpen(
                        ServerHandshake handshakeData
                ) {

                    System.out.println(
                            "? ?� k?t n?i t?i server!"
                    );
                }

                @Override
                public void onMessage(String message) {
//                    // 1. Debug th�ng minh:
//                    // N?u tin nh?n qu� d�i (th??ng l� c� ch?a ?nh), ch? in 200 k� t? ??u ?? xem Type v� Status
//                    if (message != null && message.length() > 200) {
//                        System.out.println("? [T? Server] (G�i tin l?n): " + message.substring(0, 200) + "... [T?ng: " + message.length() + " k� t?]");
//                    } else {
//                        System.out.println("? [T? Server]: " + message);
//                    }

                    // 2. V?n dispatch b�nh th??ng ?? x? l� logic
                    ClientMessageDispatcher.dispatch(message);
                }

                @Override
                public void onClose(
                        int code,
                        String reason,
                        boolean remote
                ) {

                    System.out.println("? M?t k?t n?i");

                    System.out.println(
                            "Code: " + code
                    );

                    System.out.println(
                            "Reason: " + reason
                    );
                }

                @Override
                public void onError(Exception ex) {

                    System.err.println(
                            "? L?i m?ng:"
                    );

                    ex.printStackTrace();
                }
            };

            System.out.println(
                    "? ?ang k?t n?i t?i server..."
            );

            webSocketClient.connectBlocking();

            System.out.println(
                    "? K?t n?i ho�n t?t."
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void sendCommand(String command) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(command);

            // Debug th�ng minh: N?u g�i tin qu� d�i th� ch? in ?? d�i th�i
            if (command.length() > 200) {
                System.out.println("? [Client] ?� g?i g�i tin l?n (Size: " + command.length() + " chars)");
            } else {
                System.out.println("? [Client] ?� g?i: " + command);
            }

        } else {
            System.err.println("? Ch?a k?t n?i m?ng!");
        }
    }

    public static boolean isConnected() {

        return webSocketClient != null
                && webSocketClient.isOpen();
    }

    // H�m ?? c�c m�n h�nh kh�c g?n tai nghe v�o
    public static void setListener(MessageListener listener) {
        currentListener = listener;
    }
}