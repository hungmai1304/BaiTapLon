package com.auction.server;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.server.model.ServerContext;
import com.auction.server.service.AuctionManager;
import com.google.gson.Gson;

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.Timer;
import java.util.TimerTask;

public class AuctionWebSocketServer extends WebSocketServer {

    private final Gson gson = new Gson();
    private final MessageDispatcher dispatcher;
    private Timer auctionTimer; //  THÊM - Timer để kiểm tra hết giờ

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));

        // Khởi tạo ServerContext
        ServerContext context = ServerContext.getInstance();
        context.initData(this, null); // currentProduct sẽ được set bởi AuctionManager

        //  THÊM - Tạo data mẫu (sau này thay bằng database)
        initSampleProducts();

        dispatcher = new MessageDispatcher(gson, context);
    }

    // ==========  THÊM MỚI - Tạo sản phẩm mẫu ==========
    private void initSampleProducts() {
        ServerContext context = ServerContext.getInstance();

        // Sản phẩm 1
        Product p1 = new Product();
        p1.setId("P001");
        p1.setName("Laptop Gaming Asus ROG");
        p1.setCategory("Đồ Điện Tử");
        p1.setStartPrice(20000000);
        p1.setCurrentPrice(20000000);
        p1.setStepPrice(500000);
        p1.setStatus(ProductStatus.AVAILABLE); //  Chờ được chọn

        // Sản phẩm 2
        Product p2 = new Product();
        p2.setId("P002");
        p2.setName("Mô hình Iron Man 1:1");
        p2.setCategory("Đồ Sưu Tầm");
        p2.setStartPrice(5000000);
        p2.setCurrentPrice(5000000);
        p2.setStepPrice(100000);
        p2.setStatus(ProductStatus.AVAILABLE);

        // Sản phẩm 3
        Product p3 = new Product();
        p3.setId("P003");
        p3.setName("iPhone 16 Pro Max");
        p3.setCategory("Điện Thoại");
        p3.setStartPrice(30000000);
        p3.setCurrentPrice(30000000);
        p3.setStepPrice(1000000);
        p3.setStatus(ProductStatus.AVAILABLE);

        // Thêm vào ServerContext
        context.addProduct(p1);
        context.addProduct(p2);
        context.addProduct(p3);

        System.out.println("📦 [Server] Đã tạo " + context.getProductList().size() + " sản phẩm mẫu!");
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("✅ Client vào phòng: " + conn.getRemoteSocketAddress());

        conn.send("{\"type\":\"SYSTEM_NOTIFICATION\", \"message\":\"Chào mừng bạn đến với sàn đấu giá!\"}");
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

        int remainingUsers = getConnections().size();
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + remainingUsers + "}");

        ServerContext.getInstance().removeUser(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("❌ WebSocket Error:");
        ex.printStackTrace();
    }

    // ========== ✅ THÊM MỚI - Khởi động AuctionManager khi server sẵn sàng ==========
    @Override
    public void onStart() {
        System.out.println("🚀 WebSocket Server đã sẵn sàng!");

        // 1. Chọn sản phẩm đầu tiên lên đấu giá
        AuctionManager manager = AuctionManager.getInstance();
        manager.pickNextProduct();

        // 2. Tạo Timer kiểm tra hết giờ mỗi 1 giây
        auctionTimer = new Timer("AuctionTimer", true); // daemon thread
        auctionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                manager.checkAndEndExpiredAuctions();
            }
        }, 0, 1000); // Chạy ngay lập tức, lặp lại mỗi 1 giây

        System.out.println("⏰ [Server] Timer đấu giá đã khởi động - kiểm tra mỗi giây!");
    }

    // ========== ✅ THÊM MỚI - Dọn dẹp khi server tắt ==========
    public void shutdown() {
        if (auctionTimer != null) {
            auctionTimer.cancel();
            System.out.println("⏰ [Server] Timer đã dừng!");
        }
        try {
            this.stop();
            System.out.println("🛑 [Server] WebSocket Server đã dừng!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}