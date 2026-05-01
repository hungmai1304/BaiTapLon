package com.auction.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;

public class NetworkClient {
    private static final String SERVER_URL = "wss://baitaplon-qegw.onrender.com";
    private static WebSocketClient webSocketClient;

    // Giao diện để "bắn" dữ liệu về cho Controller
    public interface MessageListener {
        void onMessageReceived(String message);
    }

    private static MessageListener currentListener;

    // Controller sẽ gọi hàm này để đăng ký nhận tin nhắn
    public static void setListener(MessageListener listener) {
        currentListener = listener;
    }

    // Hàm mở kết nối 24/7
    public static void connectAndKeepAlive() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            return; // Nếu đang mở rồi thì thôi không mở lại
        }

        try {
            webSocketClient = new WebSocketClient(new URI(SERVER_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("🟢 Đã thiết lập kết nối Real-time!");
                    // Vừa vào là xin thông tin sản phẩm luôn
                    send("GET_CURRENT");
                }

                @Override
                public void onMessage(String message) {
                    // CÓ TIN NHẮN TỪ SERVER -> Đẩy về cho Controller
                    if (currentListener != null) {
                        currentListener.onMessageReceived(message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("🔴 Mất kết nối: " + reason);
                    // Có thể thêm logic tự động kết nối lại ở đây nếu muốn
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("❌ Lỗi mạng: " + ex.getMessage());
                }
            };
            webSocketClient.connect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Hàm để Client gửi lệnh lên Server (ví dụ: "BID:50000")
    public static void sendCommand(String command) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(command);
        } else {
            System.err.println("⚠️ Chưa kết nối mạng, không thể gửi lệnh!");
        }
    }
}