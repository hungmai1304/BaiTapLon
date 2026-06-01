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
import java.util.Collection;
import java.util.logging.Logger;
import java.util.concurrent.CompletableFuture;

import com.auction.server.service.AuctionManager;

public class AuctionWebSocketServer extends WebSocketServer {
    private static final Logger LOGGER = Logger.getLogger(AuctionWebSocketServer.class.getName());

    private final Gson gson;
    private final MessageDispatcher dispatcher;

    // =========================================================================
    // 1. Constructor chính chạy linh hoạt cho Tailscale / Render (ĐÃ SỬA SẠCH LỖI)
    // =========================================================================
    public AuctionWebSocketServer(InetSocketAddress address) {
        // Gọi lên class cha để tự động cấu hình Selector mạng đa luồng mặc định cực kỳ tối ưu
        super(address);
        this.gson = initGson();

        // Khởi tạo ngữ cảnh Server
        ServerContext context = ServerContext.getInstance();
        context.initData(this);
        dispatcher = new MessageDispatcher(gson, context);
    }

    // =========================================================================
    // 2. Constructor cũ nhận vào port để không làm gãy code chỗ khác
    // =========================================================================
    public AuctionWebSocketServer(int port) {
        this(new InetSocketAddress(port));
    }

    // =========================================================================
    // 3. Hàm khởi tạo Gson dùng chung cho cả class
    // =========================================================================
    private Gson initGson() {
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

        try {
            // Gửi lời chào riêng cho Client vừa kết nối thành công
            conn.send("{\"type\":\"SYSTEM_NOTIFICATION\", \"message\":\"Chào mừng bạn đến với sàn đấu giá!\"}");
        } catch (Exception e) {
            LOGGER.warning("Không thể gửi tin nhắn chào mừng: " + e.getMessage());
        }

        // Phát loa số lượng người dùng độc lập, hoàn toàn bất đồng bộ
        triggerAsyncUserCountUpdate();
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        // Kiểm tra an toàn tin nhắn trống
        if (message == null || message.trim().isEmpty()) return;

        // Debug thông minh: Không in cả cục Base64 lớn làm nghẽn I/O Console
        if (message.length() > 200) {
            LOGGER.info("[Server Nhận] Gói tin lớn: " + message.substring(0, 150) + "... [Độ dài: " + message.length() + "]");
        } else {
            LOGGER.info("[Server Nhận]: " + message);
        }

        try {
            dispatcher.dispatch(conn, message);
        } catch (Exception e) {
            LOGGER.severe("Lỗi xử lý message từ client " + conn.getRemoteSocketAddress() + ": " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        LOGGER.info("Client thoát: " + (conn != null ? conn.getRemoteSocketAddress() : "Unknown"));

        // 1. Xóa thông tin chuỗi email map với Connection và dọn dẹp RAM trước
        if (conn != null) {
            ServerContext.getInstance().removeUser(conn);
            ServerContext.getInstance().removeOnlineUserObject(conn);
        }

        // 2. Cập nhật số lượng kết nối bất đồng bộ sang luồng khác ngay để giải phóng onClose
        triggerAsyncUserCountUpdate();
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        LOGGER.severe("[WebSocket] Lỗi kết nối tại: " + (conn != null ? conn.getRemoteSocketAddress() : "Hệ thống"));
        if (ex != null) {
            LOGGER.severe("Chi tiết lỗi: " + ex.getMessage());
        }
    }

    @Override
    public void onStart() {
        LOGGER.info("[WebSocketServer] WebSocket Server đã sẵn sàng và đang lắng nghe!");
    }

    public void shutdown() {
        try {
            this.stop();
            LOGGER.info("[WebSocketServer] WebSocket Server đã dừng hoàn toàn!");
        } catch (Exception e) {
            LOGGER.severe("Lỗi khi dừng WebSocket Server: " + e.getMessage());
        }
    }

    // =========================================================================
    // HÀM BỔ SUNG: Phát loa số lượng người dùng bất đồng bộ (CHỐNG SẬP TUYỆT ĐỐI)
    // =========================================================================
    private void triggerAsyncUserCountUpdate() {
        // Đẩy toàn bộ quá trình xử lý I/O mạng ra luồng ngầm độc lập nhằm giải phóng luồng xử lý chính ngay lập tức
        CompletableFuture.runAsync(() -> {
            try {
                int count = getConnections().size();
                String jsonUpdate = "{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + count + "}";

                // 1. Đồng bộ danh sách online sang luồng Admin
                ServerContext.getInstance().broadcastOnlineUsersToAdmins();

                // 2. Lấy bản sao danh sách kết nối hiện tại
                Collection<WebSocket> conns = getConnections();

                // 3. Gửi tin tới từng client một cách biệt lập hoàn toàn
                for (WebSocket client : conns) {
                    if (client != null && client.isOpen()) {
                        try {
                            client.send(jsonUpdate);
                        } catch (Exception e) {
                            // Nếu một client mạng lag đứt kết nối ngầm, lỗi ghi mạng sẽ bị bỏ qua lập tức,
                            // tuyệt đối không treo luồng và không ảnh hưởng tới việc nhận tin của các máy khác.
                            LOGGER.warning("[AsyncBroadcast] Không thể gửi số lượng kết nối tới 1 client lag: " + e.getMessage());
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.severe("[AsyncBroadcast] Lỗi hệ thống trong luồng phát loa: " + e.getMessage());
            }
        });
    }
}