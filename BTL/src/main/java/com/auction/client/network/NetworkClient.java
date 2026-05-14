package com.auction.client.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;

public class NetworkClient {

    // Production
    private static final String SERVER_URL ="wss://baitaplon-qegw.onrender.com";

    // Local
    //private static final String SERVER_URL ="ws://localhost:10000";

    private static WebSocketClient webSocketClient;
    private static MessageListener currentListener;

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
//                    // 1. Debug thông minh:
//                    // Nếu tin nhắn quá dài (thường là có chứa ảnh), chỉ in 200 ký tự đầu để xem Type và Status
//                    if (message != null && message.length() > 200) {
//                        System.out.println("📩 [Từ Server] (Gói tin lớn): " + message.substring(0, 200) + "... [Tổng: " + message.length() + " ký tự]");
//                    } else {
//                        System.out.println("📩 [Từ Server]: " + message);
//                    }

                    // 2. Vẫn dispatch bình thường để xử lý logic
                    ClientMessageDispatcher.dispatch(message);
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
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(command);

            // Debug thông minh: Nếu gói tin quá dài thì chỉ in độ dài thôi
            if (command.length() > 200) {
                System.out.println("🚀 [Client] Đã gửi gói tin lớn (Size: " + command.length() + " chars)");
            } else {
                System.out.println("🚀 [Client] Đã gửi: " + command);
            }

        } else {
            System.err.println("❌ Chưa kết nối mạng!");
        }
    }

    public static boolean isConnected() {

        return webSocketClient != null
                && webSocketClient.isOpen();
    }

    // Hàm để các màn hình khác gắn tai nghe vào
    public static void setListener(MessageListener listener) {
        currentListener = listener;
    }
}