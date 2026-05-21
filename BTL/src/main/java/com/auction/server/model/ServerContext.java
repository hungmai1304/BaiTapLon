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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

    // Thread pool xử lý ngầm việc gánh tải Broadcast (Dịch JSON + I/O mạng)
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(
            Math.max(4, Runtime.getRuntime().availableProcessors())
    );

    // 1. Quản lý User Online (Key: String đại diện email/id, Value: WebSocket)
    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    // KỸ THUẬT MỚI: Bản đồ ngược để biến hàm getUserByConn từ O(N) thành O(1) tuyệt đối
    private final Map<WebSocket, String> connToUserKey = new ConcurrentHashMap<>();

    // 2. Quản lý thông tin User kết hợp với kết nối WebSocket tương ứng (Dạng Object)
    private final Map<WebSocket, User> onlineUserObjects = new ConcurrentHashMap<>();

    // Danh sách phiên đấu giá đang diễn ra trên RAM (Chuyển sang Map để đạt O(1) khi update/remove)
    private final Map<String, Auction> activeAuctionsMap = new ConcurrentHashMap<>();

    // Danh sách những người đăng ký nhận tin TikTok (Listener)
    private final Set<WebSocket> tiktokListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // --- THIẾT KẾ SINGLETON (Cải tiến giữ nguyên tương thích ngược) ---
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
    public AuctionWebSocketServer getServer() {
        return server;
    }

    // --- Quản lý Phiên đấu giá (Cũ trả về List, ta giữ nguyên kiểu List để không lỗi code chỗ khác) ---
    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctionsMap.values());
    }

    public void addAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            activeAuctionsMap.put(auction.getId(), auction);
            System.out.println("[ServerContext] Đã thêm Phiên Đấu Giá (ID: " + auction.getId() + ") vào RAM.");
            broadcastAuctionUpdate(); // Phát loa cập nhật
        }
    }

    public void removeAuction(String auctionId) {
        if (auctionId == null) return;
        Auction removed = activeAuctionsMap.remove(auctionId);
        if (removed != null) {
            System.out.println("[ServerContext] Đã xóa Phiên Đấu Giá (ID: " + auctionId + ") khỏi RAM.");
            broadcastAuctionUpdate();
        }
    }

    public void updateAuction(Auction updatedAuction) {
        if (updatedAuction == null || updatedAuction.getId() == null) return;

        // Thao tác ghi đè trực tiếp Map tốn O(1), loại bỏ hoàn toàn vòng lặp for và synchronized cũ
        activeAuctionsMap.put(updatedAuction.getId(), updatedAuction);
        System.out.println("[ServerContext] Đã cập nhật Auction ID: " + updatedAuction.getId());
        broadcastAuctionUpdate();
    }

    public Auction getAuctionByProductId(String productId) {
        if (productId == null) return null;
        // Do tìm theo ProductId (không phải key chính), ta quét Stream trên values của Map (An toàn, không lo Crash)
        return activeAuctionsMap.values().stream()
                .filter(a -> a.getProduct() != null && productId.equals(a.getProduct().getId()))
                .findFirst()
                .orElse(null);
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
     * Broadcast danh sách đấu giá mới nhất tới tất cả Listeners (Nâng cấp chạy ASYNC ngầm)
     */
    private void broadcastAuctionUpdate() {
        if (tiktokListeners.isEmpty()) return;

        // Đẩy toàn bộ tác vụ dịch JSON và gửi qua mạng vào Thread Pool để không làm nghẽn luồng xử lý chính
        asyncExecutor.submit(() -> {
            try {
                Response response = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Cập nhật danh sách đấu giá.");
                response.getData().put("auctionList", new ArrayList<>(activeAuctionsMap.values()));

                String json = gson.toJson(response);
                System.out.println("[AsyncBroadcast] Broadcast cập nhật tới " + tiktokListeners.size() + " listeners.");

                Iterator<WebSocket> it = tiktokListeners.iterator();
                while (it.hasNext()) {
                    WebSocket conn = it.next();
                    if (conn != null && conn.isOpen()) {
                        conn.send(json);
                    } else {
                        it.remove(); // Tự dọn dẹp connection chết
                    }
                }
            } catch (Exception e) {
                System.err.println("[AsyncBroadcast] Lỗi khi xử lý phát loa đấu giá: " + e.getMessage());
            }
        });
    }

    // --- Quản lý User Connection (Dạng chuỗi ID/Email) ---
    public void addOnlineUser(String userId, WebSocket conn) {
        if (userId == null || conn == null) return;
        onlineUsers.put(userId, conn);
        connToUserKey.put(conn, userId); // Lưu vào bản đồ ngược để phục vụ getUserByConn với tốc độ O(1)
        System.out.println("[ServerContext] User [" + userId + "] đã online!");
    }

    public void removeUser(WebSocket conn) {
        if (conn == null) return;

        // Xóa bằng bản đồ ngược cực nhanh
        String userId = connToUserKey.remove(conn);
        if (userId != null) {
            onlineUsers.remove(userId);
        } else {
            // Cơ chế phòng thủ phụ nếu bản đồ ngược chưa kịp lưu
            onlineUsers.entrySet().removeIf(entry -> entry.getValue().equals(conn));
        }
        removeTikTokListener(conn); // Dọn dẹp cả listener nếu có
    }

    /**
     * ĐÃ TỐI ƯU: Tìm kiếm ID/Email từ kết nối mạng đạt tốc độ O(1) tuyệt đối
     */
    public String getUserByConn(WebSocket conn) {
        if (conn == null) return null;
        return connToUserKey.get(conn); // Lấy trực tiếp từ Map, không duyệt Stream giải phóng CPU hoàn toàn
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
     * Cập nhật trạng thái (status) trực tiếp trên RAM cho một User dựa theo Email.
     */
    public void updateUserStatusInRam(String email, String newStatus) {
        if (email == null || email.trim().isEmpty()) return;

        for (User user : onlineUserObjects.values()) {
            if (user != null && email.equalsIgnoreCase(user.getEmail())) {
                user.setStatus(newStatus);
                System.out.println("[ServerContext] Đã cập nhật trạng thái của User [" + email + "] trên RAM thành: " + newStatus);
                broadcastOnlineUsersToAdmins(); // Đồng bộ ngay lập tức sang Admin Client
                break;
            }
        }
    }

    /**
     * Xóa một User đang online dựa vào Email, dọn dẹp sạch sẽ ở CẢ 2 DANH SÁCH QUẢN LÝ
     */
    public void removeOnlineUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return;

        WebSocket connToRemove = null;

        // 1. Kiểm tra và xóa trong bản đồ onlineUsers
        if (onlineUsers.containsKey(email)) {
            connToRemove = onlineUsers.remove(email);
            if (connToRemove != null) {
                connToUserKey.remove(connToRemove);
            }
        } else {
            // Quét tìm trong danh sách Object để lấy kết nối ra
            for (Map.Entry<WebSocket, User> entry : onlineUserObjects.entrySet()) {
                if (entry.getValue() != null && email.equalsIgnoreCase(entry.getValue().getEmail())) {
                    connToRemove = entry.getKey();
                    break;
                }
            }
            if (connToRemove != null) {
                onlineUsers.values().remove(connToRemove);
                connToUserKey.remove(connToRemove);
            }
        }

        // 2. Xóa khỏi bản đồ đối tượng onlineUserObjects và các dịch vụ lắng nghe
        if (connToRemove != null) {
            onlineUserObjects.remove(connToRemove);
            removeTikTokListener(connToRemove); // Dọn dẹp luôn các bộ lắng nghe TikTok nếu có
            System.out.println("[ServerContext] Đã xóa hoàn toàn User có email [" + email + "] khỏi cả 2 danh sách online.");

            // 3. Phát tín hiệu cập nhật (Broadcast) đồng bộ danh sách mới về cho tất cả Admin
            broadcastOnlineUsersToAdmins();
        }
    }

    /**
     * Hàm Broadcast gửi danh sách toàn bộ người dùng đang online tới tất cả các kết nối là ADMIN (Nâng cấp chạy ASYNC)
     */
    public void broadcastOnlineUsersToAdmins() {
        // Đẩy việc xử lý quét và gửi tin cho Admin sang Thread Pool riêng biệt
        asyncExecutor.submit(() -> {
            try {
                Response response = new Response(MessageType.GET_ONLINE_USERS_RESPONSE, "SUCCESS", "Cập nhật danh sách user online.");
                response.getData().put("userList", getOnlineUserList());
                String jsonResponse = gson.toJson(response);

                onlineUsers.forEach((email, conn) -> {
                    if (conn != null && conn.isOpen() && email.toLowerCase().endsWith("@admin.com")) {
                        conn.send(jsonResponse);
                        System.out.println("[AsyncAdminBroadcast] Đã đẩy danh sách Online Users tới Admin: " + email);
                    }
                });
            } catch (Exception e) {
                System.err.println("[AsyncAdminBroadcast] Lỗi khi xử lý gửi tin tới Admin: " + e.getMessage());
            }
        });
    }

    public Map<WebSocket, User> getOnlineUserObjects() {
        return this.onlineUserObjects;
    }
}