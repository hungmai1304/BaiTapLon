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
import java.util.logging.Logger;

import com.auction.server.service.AuctionManager;

public class AuctionWebSocketServer extends WebSocketServer {
    private static final Logger LOGGER = Logger.getLogger(AuctionWebSocketServer.class.getName());

    private final Gson gson;
    private final MessageDispatcher dispatcher;

    // --- ĐÃ XÓA BIẾN auctionTimer THỪA THÃI Ở ĐÂY ---

    // =========================================================================
    // 1. Thêm constructor nhận vào InetSocketAddress để chạy Tailscale/Render linh hoạt
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
    // 2. GIỮ LẠI (TÙY CHỌN): Constructor cũ nhận vào port để không làm gãy code chỗ khác
    // =========================================================================
    public AuctionWebSocketServer(int port) {
        this(new InetSocketAddress(port));
    }

    // =========================================================================
    // 3. TÁCH BIỆT: Hàm khởi tạo Gson để tái sử dụng ở các constructor, tránh trùng lặp code
    // =========================================================================
    private Gson initGson() {
        // Thay thế bằng Lambda Expression cho gọn gàng và tránh sinh lớp ẩn danh $
        return new GsonBuilder()
                .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                        new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
                .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                        LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .create();
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        LOGGER.info("⚡ Client vào phòng: " + conn.getRemoteSocketAddress());
        conn.send("{\"type\":\"SYSTEM_NOTIFICATION\", \"message\":\"Chào mừng bạn đến với sàn đấu giá!\"}");
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + getConnections().size() + "}");
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Debug thông minh: Không in cả cục Base64
        if (message != null && message.length() > 200) {
            LOGGER.info("[Server Nhận] Gói tin lớn: " + message.substring(0, 150) + "... [Độ dài: " + message.length() + "]");
        } else {
            LOGGER.info("[Server Nhận]: " + message);
        }

        try {
            dispatcher.dispatch(conn, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi xử lý message:");
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("Client thoát: " + (conn != null ? conn.getRemoteSocketAddress() : "Unknown"));

        // Gửi số lượng kết nối thô còn lại phòng hờ hệ thống đo tải
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + getConnections().size() + "}");

        // 1. Xóa thông tin chuỗi email map với Connection
        ServerContext.getInstance().removeUser(conn);

        // VIẾT THÊM: Xóa object người dùng ra khỏi danh sách quản lý Online RAM
        ServerContext.getInstance().removeOnlineUserObject(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.severe("[WebSocket] WebSocket Error:");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        LOGGER.info("[WebSocketServer] WebSocket Server đã sẵn sàng!");
        // --- ĐÃ XÓA BỎ HOÀN TOÀN ĐOẠN ĐẶT LỊCH QUÉT TIMER 1 GIÂY THỪA THÃI TẠI ĐÂY ---
        // Giờ đây AuctionManager sẽ tự kiểm soát vòng đời đóng phiên qua các luồng bắn tỉa Real-time
    }

    public void shutdown() {
        // --- ĐÃ XÓA ĐOẠN auctionTimer.cancel() VÌ KHÔNG CÒN SỬ DỤNG ---
        try {
            this.stop();
            LOGGER.info("[WebSocketServer] WebSocket Server đã dừng!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}