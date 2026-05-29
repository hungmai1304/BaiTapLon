package com.auction.server;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.auction.Auction;
import com.auction.server.model.ServerContext;
import com.google.gson.*;

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

    private final Gson gson;
    private final MessageDispatcher dispatcher;
    private Timer auctionTimer;

    // =========================================================================
    // 1. THAY ĐỔI CHÍNH: Thêm constructor nhận vào InetSocketAddress để chạy Tailscale/Render linh hoạt
    // =========================================================================
    public AuctionWebSocketServer(InetSocketAddress address) {
        super(address);
        this.gson = initGson();

        // Khởi tạo ServerContext
        ServerContext context = ServerContext.getInstance();
        context.initData(this);

        // Truyền gson đã cấu hình vào dispatcher
        dispatcher = new MessageDispatcher(gson, context);
    }

    // =========================================================================
    // 2. GIỮ LẠI (TÙY CHỌN): Constructor cũ nhận vào port để không làm gãy code chỗ khác (nếu có)
    // =========================================================================
    public AuctionWebSocketServer(int port) {
        this(new InetSocketAddress(port));
    }

    // =========================================================================
    // 3. TÁCH BIỆT: Hàm khởi tạo Gson để tái sử dụng ở các constructor, tránh trùng lặp code
    // =========================================================================
    private Gson initGson() {
        return new GsonBuilder()
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