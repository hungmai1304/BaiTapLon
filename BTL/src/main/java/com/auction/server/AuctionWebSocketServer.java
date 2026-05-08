package com.auction.server;

import com.auction.common.model.product.Product;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;

public class AuctionWebSocketServer extends WebSocketServer {

    private final Gson gson = new Gson();

    private final MessageDispatcher dispatcher;

    private Product currentProduct;

    public AuctionWebSocketServer(int port) {

        super(new InetSocketAddress(port));

        currentProduct = new Product();

        currentProduct.setId("P001");
        currentProduct.setName("Laptop Gaming Siêu Cấp");
        currentProduct.setCurrentPrice(100000);

        ServerContext context = ServerContext.getInstance();

        context.initData(this, currentProduct);

        dispatcher = new MessageDispatcher(gson, context);
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ Client vào phòng: " + conn.getRemoteSocketAddress());

        // Gửi lời chào riêng cho người mới vào (tùy chọn)
        conn.send("{\"type\":\"SYSTEM_NOTIFICATION\", \"message\":\"Chào mừng bạn đến với sàn đấu giá!\"}");

        // Thông báo cho tất cả những người đang online khác
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + getConnections().size() + "}");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {

        System.out.println("📩 Nhận: " + message);

        try {

            dispatcher.dispatch(conn, message);

        } catch (Exception e) {

            System.err.println("❌ Lỗi xử lý message:");
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("❌ Client thoát: " + (conn != null ? conn.getRemoteSocketAddress() : "Unknown"));

        // 1. Cập nhật số lượng người xem cho các client còn lại
        // Trừ đi chính nó vừa thoát
        int remainingUsers = getConnections().size();
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + remainingUsers + "}");

        // 2. Logic dọn dẹp trong ServerContext
        // ServerContext.getInstance().removeUser(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {

        System.err.println("❌ WebSocket Error:");

        ex.printStackTrace();
    }

    @Override
    public void onStart() {

        System.out.println("🚀 WebSocket Server đã sẵn sàng!");
    }
}