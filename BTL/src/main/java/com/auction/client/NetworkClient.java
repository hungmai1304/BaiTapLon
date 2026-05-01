package com.auction.client;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class NetworkClient {
    // Luôn sử dụng wss:// cho Render
    private static final String SERVER_URL = "wss://baitaplon-qegw.onrender.com";

    public static String sendRequest(String command) {
        // Dùng CompletableFuture để xử lý bất đồng bộ
        CompletableFuture<String> responseFuture = new CompletableFuture<>();

        try {
            WebSocketClient client = new WebSocketClient(new URI(SERVER_URL)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    System.out.println("[Network] Connected! Sending: " + command);
                    send(command);
                }

                @Override
                public void onMessage(String message) {
                    System.out.println("[Network] Received response.");
                    responseFuture.complete(message);
                    this.close(); // Đóng kết nối sau khi nhận được data
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (!responseFuture.isDone()) {
                        responseFuture.completeExceptionally(new Exception("Connection closed: " + reason));
                    }
                }

                @Override
                public void onError(Exception ex) {
                    System.err.println("[Network] Error: " + ex.getMessage());
                    responseFuture.completeExceptionally(ex);
                }
            };

            // Bắt đầu kết nối
            client.connect();

            /**
             * Cực kỳ quan trọng: Đợi tối đa 30 giây (vì Render ngủ đông cần thời gian dậy).
             * Trong Java 25, cơ chế Virtual Threads (nếu bạn dùng) sẽ chạy cực mượt với .get()
             */
            return responseFuture.get(30, TimeUnit.SECONDS);

        } catch (TimeoutException e) {
            System.err.println("[Network] Server Render quá chậm, Timeout!");
            return null;
        } catch (Exception e) {
            System.err.println("[Network] Lỗi thực thi: " + e.getMessage());
            // e.printStackTrace();
            return null;
        }
    }
}