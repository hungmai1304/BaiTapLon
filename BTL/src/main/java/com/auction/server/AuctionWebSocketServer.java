package com.auction.server;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
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
        context.initData(this, null);

        // Tạo data mẫu
        initSampleProducts();

        // Truyền gson đã cấu hình vào dispatcher
        dispatcher = new MessageDispatcher(gson, context);
    }

    private void initSampleProducts() {
        ServerContext context = ServerContext.getInstance();

        Product p1 = new Product();
        p1.setId("P001");
        p1.setName("Laptop Gaming Asus ROG");
        p1.setCategory("Điện Tử");
        p1.setStartPrice(20000000);
        p1.setCurrentPrice(20000000);
        p1.setStepPrice(500000);
        p1.setStatus(ProductStatus.ON_AUCTION);
        // Nếu class Product có trường LocalDateTime, nó sẽ không còn bị lỗi nữa

        Product p2 = new Product();
        p2.setId("P002");
        p2.setName("Mô hình Iron Man 1:1");
        p2.setCategory("Sưu Tầm");
        p2.setStartPrice(5000000);
        p2.setCurrentPrice(5000000);
        p2.setStepPrice(100000);
        p2.setStatus(ProductStatus.ON_AUCTION);

        Product p3 = new Product();
        p3.setId("P003");
        p3.setName("iPhone 16 Pro Max");
        p3.setCategory("Điện Thoại");
        p3.setStartPrice(30000000);
        p3.setCurrentPrice(30000000);
        p3.setStepPrice(1000000);
        p3.setStatus(ProductStatus.ON_AUCTION);

        context.addProduct(p1);
        context.addProduct(p2);
        context.addProduct(p3);

        System.out.println("✔ [Server] Đã tạo " + context.getProductList().size() + " sản phẩm mẫu!");
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
        broadcast("{\"type\":\"USER_COUNT_UPDATE\", \"count\":" + getConnections().size() + "}");
        ServerContext.getInstance().removeUser(conn);
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("[WebSocket] WebSocket Error:");
        ex.printStackTrace();
    }

    @Override
    public void onStart() {
        System.out.println("[WebSocketServer] WebSocket Server đã sẵn sàng!");
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