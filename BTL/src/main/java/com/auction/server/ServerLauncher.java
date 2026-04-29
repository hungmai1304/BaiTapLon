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
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    // 1. Đọc request (để tránh lỗi Broken Pipe)
                    clientSocket.getInputStream().read(new byte[1024]);

                    // 2. PHẢI gửi phản hồi HTTP hợp lệ
                    String response = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: text/plain\r\n" +
                            "Content-Length: 2\r\n" +
                            "\r\n" +
                            "OK";

                    clientSocket.getOutputStream().write(response.getBytes());
                    clientSocket.getOutputStream().flush();

                    // 3. Đóng socket ngay sau khi trả lời để giải phóng kết nối
                    clientSocket.close();

                } catch (Exception e) {
                    System.err.println("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi Server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
