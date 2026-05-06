package com.auction.server;

public class ServerLauncher {
    public static void main(String[] args) {
        // Render cấp cổng qua biến môi trường PORT, nếu chạy local thì dùng 10000
        String portStr = System.getenv("PORT");
        int port = (portStr != null) ? Integer.parseInt(portStr) : 10000;

        System.out.println("=== SERVER ĐẤU GIÁ (WEBSOCKET) ĐANG KHỞI ĐỘNG ===");
        System.out.println("Port: " + port);

        // Khởi tạo và chạy WebSocket Server
        AuctionWebSocketServer server = new AuctionWebSocketServer(port);
        server.start();

        // .start() sẽ chạy một luồng riêng, nên ServerLauncher sẽ không bị block ở đây
        System.out.println("Đang chờ Client kết nối...");

    }

}
//-----------------------------------------------------------------------------------
//1. Đọc PORT
//2. Chọn cổng
//3. Tạo WebSocket server
//4. Start server
//5. Chờ client vào
