package com.auction.server;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.auction.Auction;
import com.auction.server.model.ServerContext;
import com.google.gson.*; // Thay đổi để dùng GsonBuilder

import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.lang.reflect.Type;
import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import com.auction.server.service.AuctionManager;

public class AuctionWebSocketServer extends WebSocketServer {

    private final Gson gson; // Bỏ final khởi tạo trực tiếp
    private final MessageDispatcher dispatcher;
    private Timer auctionTimer;

    public AuctionWebSocketServer(int port) {
        super(new InetSocketAddress(port));

        // KHỞI TẠO GSON CÓ TYPE ADAPTER CHO LOCALDATETIME
        this.gson = new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                    @Override
                    public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
                        return new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    }
                })
                .registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
                    @Override
                    public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                        return LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    }
                })
                .create();

        // Khởi tạo ServerContext
        ServerContext context = ServerContext.getInstance();
        context.initData(this);

        // Tạo data mẫu
        initSampleProducts();

        // Truyền gson đã cấu hình vào dispatcher
        dispatcher = new MessageDispatcher(gson, context);
    }

    private void initSampleProducts() {
        ServerContext context = ServerContext.getInstance();

        Product p1 = new Product();
        p1.setId("P001");
        p1.setName("Gaming Laptop RTX 4090");
        p1.setCategory("Electronics");
        p1.setStartPrice(50000000);
        p1.setCurrentPrice(50000000);
        p1.setStepPrice(1000000);
        p1.setStatus(ProductStatus.ON_AUCTION);

        Product p2 = new Product();
        p2.setId("P002");
        p2.setName("Vintage Oil Painting");
        p2.setCategory("Art");
        p2.setStartPrice(12000000);
        p2.setCurrentPrice(12000000);
        p2.setStepPrice(500000);
        p2.setStatus(ProductStatus.ON_AUCTION);

        Product p3 = new Product();
        p3.setId("P003");
        p3.setName("Limited Edition Sneakers");
        p3.setCategory("Fashion");
        p3.setStartPrice(8000000);
        p3.setCurrentPrice(8000000);
        p3.setStepPrice(200000);
        p3.setStatus(ProductStatus.ON_AUCTION);

        Product p4 = new Product();
        p4.setId("P004");
        p4.setName("Tesla Model S 2024");
        p4.setCategory("Vehicles");
        p4.setStartPrice(2500000000.0);
        p4.setCurrentPrice(2500000000.0);
        p4.setStepPrice(50000000);
        p4.setStatus(ProductStatus.ON_AUCTION);

        Product p5 = new Product();
        p5.setId("P005");
        p5.setName("Luxury Penthouse District 1");
        p5.setCategory("Property");
        p5.setStartPrice(15000000000.0);
        p5.setCurrentPrice(15000000000.0);
        p5.setStepPrice(100000000);
        p5.setStatus(ProductStatus.ON_AUCTION);

        Auction a1 = new Auction();
        a1.setId(1);
        a1.setProduct(p1);
        a1.setStartPrice(p1.getStartPrice());
        a1.setCurrentPrice(p1.getStartPrice());
        a1.setStepPrice(p1.getStepPrice());
        a1.setStartTime(LocalDateTime.now());
        a1.setEndTime(LocalDateTime.now().plusHours(1));
        a1.setStatus("ACTIVE");

        Auction a2 = new Auction();
        a2.setId(2);
        a2.setProduct(p2);
        a2.setStartPrice(p2.getStartPrice());
        a2.setCurrentPrice(p2.getStartPrice());
        a2.setStepPrice(p2.getStepPrice());
        a2.setStartTime(LocalDateTime.now());
        a2.setEndTime(LocalDateTime.now().plusHours(2));
        a2.setStatus("ACTIVE");

        Auction a3 = new Auction();
        a3.setId(3);
        a3.setProduct(p3);
        a3.setStartPrice(p3.getStartPrice());
        a3.setCurrentPrice(p3.getStartPrice());
        a3.setStepPrice(p3.getStepPrice());
        a3.setStartTime(LocalDateTime.now());
        a3.setEndTime(LocalDateTime.now().plusHours(3));
        a3.setStatus("ACTIVE");

        Auction a4 = new Auction();
        a4.setId(4);
        a4.setProduct(p4);
        a4.setStartPrice(p4.getStartPrice());
        a4.setCurrentPrice(p4.getStartPrice());
        a4.setStepPrice(p4.getStepPrice());
        a4.setStartTime(LocalDateTime.now());
        a4.setEndTime(LocalDateTime.now().plusHours(4));
        a4.setStatus("ACTIVE");

        Auction a5 = new Auction();
        a5.setId(5);
        a5.setProduct(p5);
        a5.setStartPrice(p5.getStartPrice());
        a5.setCurrentPrice(p5.getStartPrice());
        a5.setStepPrice(p5.getStepPrice());
        a5.setStartTime(LocalDateTime.now());
        a5.setEndTime(LocalDateTime.now().plusHours(5));
        a5.setStatus("ACTIVE");

        context.addAuction(a1);
        context.addAuction(a2);
        context.addAuction(a3);
        context.addAuction(a4);
        context.addAuction(a5);

        System.out.println("✔ [Server] Created " + context.getActiveAuctions().size() + " English sample products!");
    }



    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        System.out.println("⚡ Client vào phòng: " + conn.getRemoteSocketAddress());
        conn.send("{\"type\":\"SYSTEM_NOTIFICATION\", \"message\":\"Chào mừng bạn đến với sàn đấu giá!\"}");
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + getConnections().size() + "}");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Debug thông minh: Không in cả cục Base64
        if (message != null && message.length() > 200) {
            System.out.println("[Server Nhận] Gói tin lớn: " + message.substring(0, 150) + "... [Độ dài: " + message.length() + "]");
        } else {
            System.out.println("[Server Nhận]: " + message);
        }

        try {
            dispatcher.dispatch(conn, message);
        } catch (Exception e) {
            System.err.println("Lỗi xử lý message:");
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client thoát: " + (conn != null ? conn.getRemoteSocketAddress() : "Unknown"));

        // Gửi số lượng kết nối thô còn lại phòng hờ hệ thống đo tải
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + getConnections().size() + "}");

        // 1. Xóa thông tin chuỗi email map với Connection
        ServerContext.getInstance().removeUser(conn);

        // VIẾT THÊM: Xóa object người dùng ra khỏi danh sách quản lý Online RAM
        // Hàm này bên trong ServerContext đã tích hợp sẵn lệnh broadcastOnlineUsersToAdmins() để làm mới UI Admin
        ServerContext.getInstance().removeOnlineUserObject(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WebSocket] WebSocket Error:");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("[WebSocketServer] WebSocket Server đã sẵn sàng!");

        // Khởi tạo và chạy Timer kiểm tra đấu giá hết hạn (mỗi 1 giây)
        auctionTimer = new Timer(true);
        auctionTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    AuctionManager.getInstance().checkAndEndExpiredAuctions();
                } catch (Exception e) {
                    System.err.println("[Timer] Lỗi khi kiểm tra đấu giá hết hạn: " + e.getMessage());
                }
            }
        }, 1000, 1000);
    }

    public void shutdown() {
        if (auctionTimer != null) {
            auctionTimer.cancel();
        }
        try {
            this.stop();
            System.out.println("[WebSocketServer] WebSocket Server đã dừng!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}