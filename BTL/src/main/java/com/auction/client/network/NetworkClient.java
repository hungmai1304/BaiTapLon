package com.auction.client.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class NetworkClient {

    // Production
    private static final String SERVER_URL =
            "wss://baitaplon-qegw.onrender.com";

    // Local
    // private static final String SERVER_URL =
    //        "ws://localhost:10000";

    private static WebSocketClient webSocketClient;

    private NetworkClient() {
    }

    public static void connectAndKeepAlive() {

        if (webSocketClient != null
                && webSocketClient.isOpen()) {

            System.out.println("✅ Đã được kết nối tới server.");

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
                            "✅ Đã kết nối tới server!"
                    );
                }

                @Override
                public void onMessage(String message) {

                    System.out.println(
                            "📩 Server: " + message
                    );

                    ClientMessageDispatcher
                            .dispatch(message);
                }

                @Override
                public void onClose(
                        int code,
                        String reason,
                        boolean remote
                ) {

                    System.out.println("❌ Mất kết nối");

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
                            "❌ Lỗi mạng:"
                    );

                    ex.printStackTrace();
                }
            };

            System.out.println(
                    "🔄 Đang kết nối tới server..."
            );

            webSocketClient.connectBlocking();

            System.out.println(
                    "✅ Kết nối hoàn tất."
            );

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    public static void sendCommand(String command) {

        if (webSocketClient != null
                && webSocketClient.isOpen()) {

            webSocketClient.send(command);

            System.out.println(
                    "📤 Đã gửi: " + command
            );

        } else {

            System.err.println(
                    "❌ Chưa kết nối mạng!"
            );
        }
    }

    public static boolean isConnected() {

        return webSocketClient != null
                && webSocketClient.isOpen();
    }
}