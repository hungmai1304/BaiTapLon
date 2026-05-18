package com.auction.server;

public class ServerLauncher {

    public static void main(String[] args) {

        try {

            // Render c?p port qua bi?n m�i tr??ng
            String portStr = System.getenv("PORT");

            int port;

            if (portStr != null) {
                port = Integer.parseInt(portStr);
            } else {
                port = 10000;
            }

//            port=10000;

            System.out.println("================================");
            System.out.println("[ServerLauncher] SERVER ??U GI� ?ANG KH?I ??NG");
            System.out.println("PORT: " + port);
            System.out.println("================================");

            AuctionWebSocketServer server =
                    new AuctionWebSocketServer(port);

            server.start();

            System.out.println("[ServerLauncher] Server ?� start.");
            System.out.println("[ServerLauncher] ?ang ch? client k?t n?i...");

            // GI? SERVER S?NG TR�N RENDER
            Thread.currentThread().join();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}