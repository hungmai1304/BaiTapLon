package com.auction.server.model;

import com.auction.common.model.user.User;
import com.auction.common.utils.LocalDateTimeAdapter;
import com.auction.protocol.Response;
import com.auction.server.AuctionWebSocketServer;
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.MessageType;
import com.google.gson.*;
import org.java_websocket.WebSocket;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerContext {

    private static ServerContext instance;
    private AuctionWebSocketServer server;


    // Sử dụng Gson chung để tối ưu hiệu năng khi broadcast
    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    // Quản lý User Online
    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    // viet them:danh sach User dang online
    // viet them : getOnlineUserDangOnline, add(User), remove(User)

    // Danh sách phiên đấu giá
    private final List<Auction> activeAuctions = Collections.synchronizedList(new ArrayList<>());

    // Danh sách sản phẩm trên RAM
//    private final List<Product> productList = Collections.synchronizedList(new ArrayList<>());

    // Danh sách những người đăng ký nhận tin TikTok (Listener)
    private final Set<WebSocket> tiktokListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ServerContext() {}

    public static synchronized ServerContext getInstance() {
        if (instance == null) {
            instance = new ServerContext();
        }
        return instance;
    }

    public void initData(AuctionWebSocketServer server) {
        this.server = server;

    }

    // --- Getter/Setter cơ bản ---
    public AuctionWebSocketServer getServer() { return server; }




    // --- Quản lý Phiên đấu giá (Có Broadcast) ---
    public List<Auction> getActiveAuctions() { return activeAuctions; }

    public void addAuction(Auction auction) {
        if (auction != null) {
            activeAuctions.add(auction);
            System.out.println("[ServerContext] Đã thêm Phiên Đấu Giá (ID: " + auction.getId() + ") vào RAM.");
            broadcastAuctionUpdate();
        }
    }

    public void removeAuction(int auctionId) {
        boolean removed = activeAuctions.removeIf(a -> a.getId() == auctionId);
        if (removed) {
            System.out.println("[ServerContext] Đã xóa Phiên Đấu Giá (ID: " + auctionId + ") khỏi RAM.");
            broadcastAuctionUpdate();
        }
    }

    public void updateAuction(Auction updatedAuction) {
        if (updatedAuction == null) return;
        synchronized (activeAuctions) {
            for (int i = 0; i < activeAuctions.size(); i++) {
                if (activeAuctions.get(i).getId() == updatedAuction.getId()) {
                    activeAuctions.set(i, updatedAuction);
                    System.out.println("[ServerContext] Đã cập nhật Auction ID: " + updatedAuction.getId());
                    broadcastAuctionUpdate();
                    return;
                }
            }
        }
    }

    public Auction getAuctionByProductId(String productId) {
        if (productId == null) return null;
        synchronized (activeAuctions) {
            return activeAuctions.stream()
                    .filter(a -> a.getProduct() != null && productId.equals(a.getProduct().getId()))
                    .findFirst()
                    .orElse(null);
        }
    }

    // --- Quản lý TikTok Listeners ---
    public void addTikTokListener(WebSocket conn) {
        tiktokListeners.add(conn);
    }

    public void removeTikTokListener(WebSocket conn) {
        tiktokListeners.remove(conn);
    }

    public Set<WebSocket> getTikTokListeners() {
        return tiktokListeners;
    }

    /**
     * Broadcast danh sách đấu giá mới nhất tới tất cả Listeners
     */
    private void broadcastAuctionUpdate() {
        if (tiktokListeners.isEmpty()) return;

        // Đóng gói Response
        Response response = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Cập nhật danh sách đấu giá.");
        // Copy list để tránh ConcurrentModificationException khi GSON đang đọc
        response.getData().put("auctionList", new ArrayList<>(activeAuctions));

        String json = gson.toJson(response);
        System.out.println("[ServerContext] Broadcast cập nhật tới " + tiktokListeners.size() + " listeners.");

        Iterator<WebSocket> it = tiktokListeners.iterator();
        while (it.hasNext()) {
            WebSocket conn = it.next();
            if (conn != null && conn.isOpen()) {
                conn.send(json);
            } else {
                it.remove(); // Tự dọn dẹp connection chết
            }
        }
    }

    // --- Quản lý User Connection ---
    public void addOnlineUser(String userId, WebSocket conn) {
        onlineUsers.put(userId, conn);
        System.out.println("[ServerContext] User [" + userId + "] đã online!");
    }

    public void removeUser(WebSocket conn) {
        if (conn == null) return;
        onlineUsers.entrySet().removeIf(entry -> entry.getValue().equals(conn));
        removeTikTokListener(conn); // Dọn dẹp cả listener nếu có
    }

    public String getUserByConn(WebSocket conn) {
        return onlineUsers.entrySet().stream()
                .filter(entry -> entry.getValue().equals(conn))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public Collection<WebSocket> getConnectedClients() {
        return onlineUsers.values();
    }

    // Thêm Map này để quản lý thông tin User kết hợp với kết nối WebSocket tương ứng
    private final Map<WebSocket, User> onlineUserObjects = new ConcurrentHashMap<>();

// --- QUẢN LÝ DANH SÁCH USER DẠNG OBJECT ---

    public void addOnlineUserObject(WebSocket conn, User user) {
        if (conn != null && user != null) {
            onlineUserObjects.put(conn, user);
            System.out.println("[ServerContext] Lưu trữ thông tin đối tượng User: " + user.getUsername());
            broadcastOnlineUsersToAdmins(); // Tự động đồng bộ tới Admin khi có người vào
        }
    }

    public void removeOnlineUserObject(WebSocket conn) {
        if (conn != null && onlineUserObjects.containsKey(conn)) {
            User removedUser = onlineUserObjects.remove(conn);
            System.out.println("[ServerContext] Đã xóa đối tượng User khỏi RAM: " + (removedUser != null ? removedUser.getUsername() : "Ẩn danh"));
            broadcastOnlineUsersToAdmins(); // Tự động đồng bộ tới Admin khi có người ra
        }
    }

    public List<User> getOnlineUserList() {
        return new ArrayList<>(onlineUserObjects.values());
    }

    /**
     * Hàm Broadcast gửi danh sách toàn bộ người dùng đang online tới tất cả các kết nối là ADMIN
     */
    public void broadcastOnlineUsersToAdmins() {
        // Tạo gói tin Response đồng bộ danh sách người dùng online
        Response response = new Response(MessageType.GET_ONLINE_USERS_RESPONSE, "SUCCESS", "Cập nhật danh sách user online.");
        response.getData().put("userList", getOnlineUserList());
        String jsonResponse = gson.toJson(response);

        // Duyệt qua tất cả các kết nối đang có trên Server
        onlineUsers.forEach((email, conn) -> {
            if (conn != null && conn.isOpen() && email.toLowerCase().endsWith("@admin.com")) {
                conn.send(jsonResponse);
                System.out.println("[ServerContext] Đã đẩy danh sách Online Users tới Admin: " + email);
            }
        });
    }

}