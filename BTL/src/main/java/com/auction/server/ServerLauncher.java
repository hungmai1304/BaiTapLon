package com.auction.server;


import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerLauncher {
    public static void main(String[] args) {
        // Render cấp cổng qua biến môi trường PORT, nếu không có thì dùng 10000
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 10000;

        System.out.println("=== SERVER ĐẤU GIÁ ĐANG CHẠY ===");
        System.out.println("Port: " + port);

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // Tại đây bạn sẽ gọi đến logic xử lý đấu giá của bạn
                // Ví dụ: new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
