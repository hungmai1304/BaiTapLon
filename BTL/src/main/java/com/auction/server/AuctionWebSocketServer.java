package com.auction.server;


import org.java_websocket.server.WebSocketServer;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.WebSocket;
import java.net.InetSocketAddress;
import com.google.gson.Gson;
import com.auction.common.model.Product; // Đảm bảo import đúng class Product của bạn

public class AuctionWebSocketServer extends WebSocketServer {

    private Gson gson = new Gson();

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("🟢 Có người vào xem đấu giá: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        System.out.println("📩 Lệnh nhận được: " + message);

        // Xử lý các lệnh từ Client gửi lên
        if ("GET_CURRENT".equals(message)) {

            // TẠM THỜI: Tạo cứng 1 sản phẩm để test.
            // SAU NÀY: Bạn gọi hàm từ AuctionDao / AuctionManager ở đây nhé!
            Product currentProduct = new Product();
            // currentProduct.setId("P01");
            // currentProduct.setName("Laptop Gaming xịn");
            // ... set các thông số ...

            // Chuyển Product thành chuỗi JSON và gửi về cho Client
            String jsonResponse = gson.toJson(currentProduct);
            conn.send(jsonResponse);
            System.out.println("Đã gửi thông tin sản phẩm về Client.");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("🔴 Client đã thoát: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ Lỗi WebSocket: " + ex.getMessage());
    }

    @Override
    public void onStart() {
        System.out.println("🚀 WebSocket Server đã sẵn sàng nhận kết nối!");
    }
}