package com.auction.server;

import com.auction.common.model.auction.Auction;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerLauncher {
    private static final Logger LOGGER = Logger.getLogger(ServerLauncher.class.getName());

    public static void main(String[] args) {
        // 1. Force Server to always run in Vietnam Timezone (GMT+7)
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        try {
            // 2. Check environment configuration (Render Cloud or Local/Tailscale)
            String portStr = System.getenv("PORT");
            int port;
            InetSocketAddress address;

            if (portStr != null && !portStr.isEmpty()) {
                // RUNNING ON RENDER: Bind dynamic port provided by environment and listen on all interfaces (0.0.0.0)
                port = Integer.parseInt(portStr);
                address = new InetSocketAddress("0.0.0.0", port);

                LOGGER.info("=========================================");
                LOGGER.info("AUCTION SERVER RUNNING ON CLOUD (RENDER)");
                LOGGER.log(Level.INFO, "PORT: {0}", port);
                LOGGER.info("=========================================");
            } else {
                // RUNNING ON LOCAL/TAILSCALE: Bind to static Tailscale IP and standard Port 10000
                port = 10000;
                String tailscaleIp = "100.89.94.42";
                address = new InetSocketAddress(tailscaleIp, port);

                LOGGER.info("=========================================");
                LOGGER.info("AUCTION SERVER INITIALIZING (TAILSCALE LOCAL)");
                LOGGER.log(Level.INFO, "TAILSCALE IP: {0}", tailscaleIp);
                LOGGER.log(Level.INFO, "PORT: {0}", port);
                LOGGER.info("=========================================");
            }

            // 3. Launch WebSocket Server with the configured address above
            AuctionWebSocketServer server = new AuctionWebSocketServer(address);
            server.start();

            if (portStr != null && !portStr.isEmpty()) {
                LOGGER.info("Server started successfully on Render.");
            } else {
                LOGGER.info("Server started successfully! Client connection link:");
                LOGGER.info("   -> ws://100.89.94.42:10000");
            }
            LOGGER.info("Awaiting client connections... ");

            // =========================================================================
            // ONE-TIME CLEANUP ON SERVER STARTUP: PURGE EXPIRED DATA
            // =========================================================================
            try {
                LOGGER.info("[SystemCheck] Scanning for expired products on server startup...");
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
                LOGGER.log(Level.INFO, "[SystemCheck] Cleanup complete! Delisted {0} expired products from the database.", affectedRows);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error encountered during startup product cleanup", e);
            }
            // =========================================================================

            // 4. KEEP MAIN THREAD ALIVE (Prevents main thread from exiting and terminating the app)
            Thread.currentThread().join();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error during server startup sequence", e);
        }
    }
}