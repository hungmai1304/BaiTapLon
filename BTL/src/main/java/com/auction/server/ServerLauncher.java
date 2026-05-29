package com.auction.server;

import com.auction.common.model.auction.Auction;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;

public class ServerLauncher {

    public static void main(String[] args) {
        // 1. �p Server lu�n ch?y ? m�i gi? Vi?t Nam (GMT+7)
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        // Ép Server luôn chạy ở múi giờ Việt Nam (GMT+7) để tránh lệch giờ khi deploy lên Cloud
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        try {
            // 2. Ki?m tra m�i tr??ng (Render hay Local/Tailscale)
            String portStr = System.getenv("PORT");
            int port;
            InetSocketAddress address;

            if (portStr != null && !portStr.isEmpty()) {
                // N?U CH?Y TR�N RENDER: L?y port t? ??ng v� l?ng nghe m?i IP (0.0.0.0)
                port = Integer.parseInt(portStr);
                address = new InetSocketAddress("0.0.0.0", port);

                System.out.println("=========================================");
                System.out.println("? SERVER ??U GI� ?ANG CH?Y TR�N CLOUD (RENDER)");
                System.out.println("PORT: " + port);
                System.out.println("=========================================");
            } else {
                // N?U CH?Y LOCAL: G�n c?ng IP Tailscale v� Port 10000
                port = 10000;
                String tailscaleIp = "100.89.94.42";
                address = new InetSocketAddress(tailscaleIp, port);

                System.out.println("=========================================");
                System.out.println("? SERVER ??U GI� ?ANG KH?I ??NG (TAILSCALE LOCAL)");
                System.out.println("IP TAILSCALE: " + tailscaleIp);
                System.out.println("PORT: " + port);
                System.out.println("=========================================");
            }

            // 3. Kh?i ch?y WebSocket Server theo ??a ch? c?u h�nh ? tr�n
            AuctionWebSocketServer server = new AuctionWebSocketServer(address);
            server.start();

            if (portStr != null && !portStr.isEmpty()) {
                System.out.println("? Server ?� start th�nh c�ng tr�n Render.");
            } else {
                System.out.println("? Server ?� start th�nh c�ng! Link k?t n?i c?a client:");
                System.out.println("   ? ws://100.89.94.42:10000");
            }
            System.out.println("? ?ang ch? c�c Client k?t n?i... ");

            // =========================================================================
            // CH? CH?Y ?�NG 1 L?N DUY NH?T KHI M? SERVER ?? D?N R�C D? LI?U C?
            // =========================================================================
            try {
                System.out.println("? [SystemCheck] ?ang qu�t ki?m tra s?n ph?m h?t h?n l�c m? Server...");
                ServerContext context = ServerContext.getInstance();

                java.util.List<String> expiredProductIds = new java.util.ArrayList<>();
                for (Auction a : context.getActiveAuctions()) {
                    if (a.getProduct() != null && a.getProduct().getEndTime() != null) {
                        if (a.getProduct().getEndTime().isBefore(LocalDateTime.now())) {
                            expiredProductIds.add(a.getProduct().getId());
                        }
                    }
                }
                for (String id : expiredProductIds) {
                    context.removeAuctionByProductId(id);
                }

                int affectedRows = ProductDao.getInstance().autoExpireProducts();
                System.out.println("? [SystemCheck] Ho�n th�nh qu�t d?n! ?� h? s�n " + affectedRows + " s?n ph?m h?t h?n d??i DB.");

            } catch (Exception e) {
                System.err.println("? L?i khi qu�t d?n l�c m? Server: " + e.getMessage());
            }
            // =========================================================================

            // 6. GI? SERVER S?NG (Tr�nh Thread main k?t th�c l�m s?p app)
            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}