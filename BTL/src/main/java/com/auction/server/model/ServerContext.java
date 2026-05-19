package com.auction.server.model;

import com.auction.common.model.user.User;
import com.auction.protocol.Response;
import com.auction.server.AuctionWebSocketServer;
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

    // 1. Quản lý User Online (Key: String đại diện email/id, Value: WebSocket)
    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    // 2. Quản lý thông tin User kết hợp với kết nối WebSocket tương ứng (Dạng Object)
    private final Map<WebSocket, User> onlineUserObjects = new ConcurrentHashMap<>();

    // Danh sách phiên đấu giá đang diễn ra trên RAM
    private final List<Auction> activeAuctions = Collections.synchronizedList(new ArrayList<>());

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

    // --- Quản lý User Connection (Dạng chuỗi ID/Email) ---
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


    // --- QUẢN LÝ DANH SÁCH USER DẠNG OBJECT ---

    /**
     * Thêm đối tượng User online vào RAM ứng với kết nối mạng
     */
    public void addOnlineUserObject(WebSocket conn, User user) {
        if (conn != null && user != null) {
            onlineUserObjects.put(conn, user);
            System.out.println("[ServerContext] Lưu trữ thông tin đối tượng User: " + user.getUsername() + " | Trạng thái: " + user.getStatus());
            broadcastOnlineUsersToAdmins(); // Tự động đồng bộ tới Admin khi có người vào
        }
    }

    /**
     * Xóa đối tượng User ra khỏi RAM khi ngắt kết nối mạng dựa trên WebSocket
     */
    public void removeOnlineUserObject(WebSocket conn) {
        if (conn != null && onlineUserObjects.containsKey(conn)) {
            User removedUser = onlineUserObjects.remove(conn);
            System.out.println("[ServerContext] Đã xóa đối tượng User khỏi RAM: " + (removedUser != null ? removedUser.getUsername() : "Ẩn danh"));
            broadcastOnlineUsersToAdmins(); // Tự động đồng bộ tới Admin khi có người ra
        }
    }

    /**
     * Lấy danh sách toàn bộ đối tượng User đang trực tuyến dưới dạng List công khai
     */
    public List<User> getOnlineUserList() {
        return new ArrayList<>(onlineUserObjects.values());
    }

    /**
     * Tìm kiếm đối tượng thông tin User dựa trên kết nối mạng WebSocket hiện tại
     */
    public User getUserByConnObject(WebSocket conn) {
        if (conn == null) return null;
        return onlineUserObjects.get(conn);
    }

    /**
     * VIẾT THÊM: Cập nhật trạng thái (status) trực tiếp trên RAM cho một User dựa theo Email.
     * Hàm này cực kỳ hữu ích khi Admin ra lệnh BAN/LOCK tài khoản, trạng thái sẽ đổi ngay lập tức trên RAM
     * trước khi hệ thống ngắt kết nối hoặc gửi danh sách cập nhật về giao diện JavaFX của Admin.
     */
    public void updateUserStatusInRam(String email, String newStatus) {
        if (email == null || email.trim().isEmpty()) return;

        for (User user : onlineUserObjects.values()) {
            if (user != null && email.equalsIgnoreCase(user.getEmail())) {
                user.setStatus(newStatus);
                System.out.println("[ServerContext] Đã cập nhật trạng thái của User [" + email + "] trên RAM thành: " + newStatus);
                broadcastOnlineUsersToAdmins(); // Đồng bộ ngay lập tức bảng danh sách của Admin Client
                break;
            }
        }
    }

    /**
     * Xóa một User đang online dựa vào Email, dọn dẹp sạch sẽ ở CẢ 2 DANH SÁCH QUẢN LÝ
     * @param email Email của tài khoản người dùng cần xóa trạng thái online
     */
    public void removeOnlineUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return;

        WebSocket connToRemove = null;

        // 1. Kiểm tra và xóa trong bản đồ onlineUsers (Trường hợp Key lưu trữ chính là Email)
        if (onlineUsers.containsKey(email)) {
            connToRemove = onlineUsers.remove(email);
        } else {
            // Trường hợp Key của onlineUsers lưu trữ bằng userId/username khác, ta sẽ quét tìm trong danh sách Object
            for (Map.Entry<WebSocket, User> entry : onlineUserObjects.entrySet()) {
                if (entry.getValue() != null && email.equalsIgnoreCase(entry.getValue().getEmail())) {
                    connToRemove = entry.getKey();
                    break;
                }
            }
            if (connToRemove != null) {
                // Xóa giá trị kết nối tương ứng ra khỏi map onlineUsers chính
                onlineUsers.values().remove(connToRemove);
            }
        }

        // 2. Xóa khỏi bản đồ đối tượng onlineUserObjects và các dịch vụ lắng nghe
        if (connToRemove != null) {
            onlineUserObjects.remove(connToRemove);
            removeTikTokListener(connToRemove); // Dọn dẹp luôn các bộ lắng nghe TikTok nếu có
            System.out.println("[ServerContext] Đã xóa hoàn toàn User có email [" + email + "] khỏi cả 2 danh sách online.");

            // 3. Phát tín hiệu cập nhật (Broadcast) đồng bộ danh sách mới về cho tất cả Admin đang bật máy Client
            broadcastOnlineUsersToAdmins();
        }
    }

    /**
     * Hàm Broadcast gửi danh sách toàn bộ người dùng đang online tới tất cả các kết nối là ADMIN
     */
    public void broadcastOnlineUsersToAdmins() {
        // Tạo gói tin Response đồng bộ danh sách người dùng online
        Response response = new Response(MessageType.GET_ONLINE_USERS_RESPONSE, "SUCCESS", "Cập nhật danh sách user online.");
        response.getData().put("userList", getOnlineUserList());
        String jsonResponse = gson.toJson(response);

        // Duyệt qua tất cả các kết nối đang có trên Server để lọc Admin nhận tin
        onlineUsers.forEach((email, conn) -> {
            if (conn != null && conn.isOpen() && email.toLowerCase().endsWith("@admin.com")) {
                conn.send(jsonResponse);
                System.out.println("[ServerContext] Đã đẩy danh sách Online Users tới Admin: " + email);
            }
        });
    }

    public Map<WebSocket, User> getOnlineUserObjects() {
        return this.onlineUserObjects;
    }
}