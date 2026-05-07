package com.auction.server;

public class ServerLauncher {

    public static void main(String[] args) {

        try {

            // Render cấp port qua biến môi trường
            String portStr = System.getenv("PORT");

            int port;

            if (portStr != null) {
                port = Integer.parseInt(portStr);
            } else {
                port = 10000;
            }

            System.out.println("================================");
            System.out.println("🚀 SERVER ĐẤU GIÁ ĐANG KHỞI ĐỘNG");
            System.out.println("PORT: " + port);
            System.out.println("================================");

            AuctionWebSocketServer server =
                    new AuctionWebSocketServer(port);

            server.start();

            System.out.println("✅ Server đã start.");
            System.out.println("⏳ Đang chờ client kết nối...");

            // GIỮ SERVER SỐNG TRÊN RENDER
            Thread.currentThread().join();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}