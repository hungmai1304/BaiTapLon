package com.auction.client.network;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkClient {
    private static final Logger LOGGER = Logger.getLogger(NetworkClient.class.getName());

    // =========================================================================
    // SERVER URL CONFIGURATION
    // =========================================================================
    // Production environment (Render)
    // private static final String SERVER_URL ="wss://baitaplon-qegw.onrender.com";

    // Local environment for standalone testing
    // private static final String SERVER_URL = "ws://localhost:10000";

    // Team testing via Tailscale: Configure your server's Tailscale IP address
    private static final String SERVER_URL = "ws://100.89.94.42:10000";
    // =========================================================================

    private static WebSocketClient webSocketClient;
    private static MessageListener currentListener;

    private NetworkClient() {
        // Private constructor to prevent instantiation of utility class
    }

    public static void connectAndKeepAlive() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            LOGGER.info("Already connected to the server.");
            return;
        }

        try {
            webSocketClient = new WebSocketClient(new URI(SERVER_URL)) {

                @Override
                public void onOpen(ServerHandshake handshakeData) {
                    LOGGER.info("Successfully connected to the server!");
                }

                @Override
                public void onMessage(String message) {
                    // Dispatch incoming message for logic processing
                    ClientMessageDispatcher.dispatch(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    LOGGER.log(Level.WARNING, "Connection closed. Code: {0}, Reason: {1}, Remote: {2}",
                            new Object[]{code, reason, remote});
                }

                @Override
                public void onError(Exception ex) {
                    LOGGER.log(Level.SEVERE, "Network connection error occurred", ex);
                }
            };

            LOGGER.log(Level.INFO, "Connecting to Tailscale server at: {0}", SERVER_URL);
            webSocketClient.connectBlocking();
            LOGGER.info("Connection process completed.");

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Fatal error during connection initialization", e);
        }
    }

    public static void sendCommand(String command) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.send(command);

            if (command.length() > 200) {
                LOGGER.log(Level.INFO, "[Client] Sent large payload (Size: {0} chars)", command.length());
            } else {
                LOGGER.log(Level.INFO, "[Client] Sent: {0}", command);
            }

        } else {
            LOGGER.severe("Network Error: Not connected to the server!");
        }
    }

    public static boolean isConnected() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    /**
     * Attach a message listener for view controllers to capture incoming text data
     */
    public static void setListener(MessageListener listener) {
        currentListener = listener;
    }
}