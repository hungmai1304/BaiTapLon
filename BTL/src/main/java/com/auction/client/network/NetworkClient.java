package com.auction.client.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class NetworkClient {

    // Production
    private static final String SERVER_URL = "wss://baitaplon-qegw.onrender.com";
//    private static final String SERVER_URL = "ws://localhost:10000";

    private static WebSocketClient webSocketClient;
    private static MessageListener currentListener;

    // Bộ điều phối để quản lý việc tự động kết nối lại
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private static boolean isReconnecting = false;

    private NetworkClient() {
    }

    public static synchronized void connectAndKeepAlive() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            System.out.println("[Network] Đã được kết nối tới server.");
            return;
        }

        if (isReconnecting) {
            return; // Tránh việc tạo nhiều luồng kết nối trùng lặp
        }

        try {
            webSocketClient = new WebSocketClient(new URI(SERVER_URL)) {

                @Override
                public void onOpen(ServerHandshake handshakeData) {
                    System.out.println("[Network] Đã kết nối tới server thành công!");
                    isReconnecting = false; // Reset trạng thái khi kết nối thành công
                }

                @Override
                public void onMessage(String message) {
                    // Chuyển tiếp tin nhắn đến Dispatcher xử lý logic
                    ClientMessageDispatcher.dispatch(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    System.out.println("[Network] Mất kết nối. Code: " + code + " | Reason: " + reason);

                    // Nếu không phải do chủ động đóng, tiến hành kết nối lại
                    if (!isReconnecting) {
                        triggerReconnect();
                    }
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[Network] Lỗi mạng xảy ra:");
                    ex.printStackTrace();
                }
            };

            // FIX LỖI 1006: Tắt kiểm tra timeout nghiêm ngặt của Client (Đặt về 0)
            // Hoặc tăng lên khoảng 60-120 giây nếu server Render phản hồi chậm
            webSocketClient.setConnectionLostTimeout(0);

            System.out.println("[Network] Đang thực hiện kết nối tới server...");

            // Sử dụng một luồng riêng để connect, giúp giao diện JavaFX không bị đơ (Freeze)
            new Thread(() -> {
                try {
                    webSocketClient.connectBlocking();
                    System.out.println("[Network] Tiến trình kết nối hoàn tất.");
                } catch (InterruptedException e) {
                    System.err.println("[Network] Tiến trình kết nối bị ngắt quãng.");
                    Thread.currentThread().interrupt();
                }
            }).start();

        } catch (Exception e) {
            e.printStackTrace();
            triggerReconnect(); // Thử lại nếu dính ngoại lệ lúc khởi tạo
        }
    }

    /**
     * Hàm kích hoạt cơ chế tự động kết nối lại sau 5 giây
     */
    private static synchronized void triggerReconnect() {
        if (isReconnecting) return;

        isReconnecting = true;
        System.out.println("[Network] Sẽ thử kết nối lại sau 5 giây...");

        scheduler.schedule(() -> {
            isReconnecting = false;
            connectAndKeepAlive();
        }, 5, TimeUnit.SECONDS);
    }

    public static void sendCommand(String command) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(command);

            // Debug thông minh thông báo gói tin lớn
            if (command.length() > 200) {
                System.out.println("[Network] Đã gửi gói tin lớn (Size: " + command.length() + " chars)");
            } else {
                System.out.println("[Network] Đã gửi: " + command);
            }
        } else {
            System.err.println("[Network] Chưa kết nối mạng! Không thể gửi lệnh.");
            // Gợi ý: Có thể tự kích hoạt kết nối lại tại đây nếu cần
            connectAndKeepAlive();
        }
    }

    public static boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public static void setListener(MessageListener listener) {
        currentListener = listener;
    }
}