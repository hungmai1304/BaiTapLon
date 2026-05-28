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

    // 1. bảng lưu email là key, để tìm conn
    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    // 2. bảng lưu conn là key, để tìm email
    private final Map<WebSocket, String> connToUserKey = new ConcurrentHashMap<>();

    // 3. bảng lưu conn là key, tìm user
    private final Map<WebSocket, User> onlineUserObjects = new ConcurrentHashMap<>();

    // lưu danh sách auction online đang diễn ra
    private final Map<String, Auction> activeAuctionsMap = new ConcurrentHashMap<>();

    // Danh sách những người đăng ký nhận tin TikTok
    private final Set<WebSocket> tiktokListeners = Collections.newSetFromMap(new ConcurrentHashMap<>());

    // constructor chỉ khởi tạo 1 lần cho servercontext
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

    // getter instance
    public AuctionWebSocketServer getServer() {
        return server;
    }

    // danh sách lưu trữ các cuộc đấu giá online, dễ dàng gửi đi cho client
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

        activeAuctionsMap.put(updatedAuction.getId(), updatedAuction);
        System.out.println("[ServerContext] Đã cập nhật Auction ID: " + updatedAuction.getId());
        broadcastAuctionUpdate();
    }

    public Auction getAuctionByProductId(String productId) {
        if (productId == null) return null;
        return activeAuctionsMap.values().stream()
                .filter(a -> a.getProduct() != null && productId.equals(a.getProduct().getId()))
                .findFirst()
                .orElse(null);
    }

    // =========================================================================
    // HÀM BỔ SUNG MỚI: Xóa phiên đấu giá trên RAM dựa vào Mã sản phẩm (Product ID)
    // Phục vụ đắc lực cho Expire Bot đồng bộ hóa từ Database lên RAM
    // =========================================================================
    public void removeAuctionByProductId(String productId) {
        if (productId == null) return;

        Auction targetAuction = getAuctionByProductId(productId);
        if (targetAuction != null) {
            activeAuctionsMap.remove(targetAuction.getId());
            System.out.println("[ServerContext] [ĐỒNG BỘ] Đã xóa Phiên Đấu Giá (ID: "
                    + targetAuction.getId() + ") của sản phẩm hết hạn (ID: " + productId + ") khỏi RAM.");
            broadcastAuctionUpdate(); // Đẩy danh sách mới về Client (Mất sàn TikTok)
        }
    }
    // =========================================================================

    // thêm và xóa cho Listeners tik tok
    public void addTikTokListener(WebSocket conn) {
        if (conn != null) tiktokListeners.add(conn);
    }

    public void removeTikTokListener(WebSocket conn) {
        if (conn != null) tiktokListeners.remove(conn);
    }

    public Set<WebSocket> getTikTokListeners() {
        return tiktokListeners;
    }

    /**
     * Broadcast danh sách đấu giá mới nhất tới tất cả Listeners (Chạy ASYNC ngầm)
     */
    private void broadcastAuctionUpdate() {
        if (tiktokListeners.isEmpty()) return;

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

    // quản lí truyền vào user id, trả về conn
    public void addOnlineUser(String userId, WebSocket conn) {
        if (userId == null || conn == null) return;
        onlineUsers.put(userId, conn);
        connToUserKey.put(conn, userId); // Lưu vào bản đồ ngược để phục vụ getUserByConn với tốc độ O(1)
        System.out.println("[ServerContext] User [" + userId + "] đã online!");
    }

    public void removeUser(WebSocket conn) {
        if (conn == null) return;

        String userId = connToUserKey.remove(conn);
        if (userId != null) {
            onlineUsers.remove(userId);
        } else {
            onlineUsers.entrySet().removeIf(entry -> entry.getValue().equals(conn));
        }
        removeOnlineUserObject(conn); // Đồng bộ dọn dẹp luôn object cache bên dưới
        removeTikTokListener(conn);
    }

    /**
     * ĐÃ TỐI ƯU: Tìm kiếm ID/Email từ kết nối mạng đạt tốc độ O(1) tuyệt đối
     */
    // trả về email, lấy conn
    public String getUserByConn(WebSocket conn) {
        if (conn == null) return null;
        return connToUserKey.get(conn);
    }

    public Collection<WebSocket> getConnectedClients() {
        return onlineUsers.values();
    }


    // các hàm quản lí cho danh sách lưu conn/user

    /**
     * TỐI ƯU BỔ SUNG: Hàm lấy thông tin User Cache nhanh từ RAM thông qua Email (Tốc độ O(1))
     * Phục vụ đắc lực cho các hàm xử lý Broadcast của Handler mà không cần chọc DB.
     */
    public User getUserCacheByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;

        // Cách tìm O(1): Lấy Connection thông qua email, rồi dùng Connection lấy Object User ra
        WebSocket conn = onlineUsers.get(email);
        if (conn != null) {
            return onlineUserObjects.get(conn);
        }

        // Dự phòng (Fallback): Nếu cơ chế map chéo bị lệch, duyệt mảng trên RAM (Vẫn nhanh hơn chọc DB)
        return onlineUserObjects.values().stream()
                .filter(u -> u != null && email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    /**
     * TỐI ƯU BỔ SUNG: Hàm lấy thông tin User Cache nhanh bằng Object WebSocket
     */
    public User getUserCacheByConn(WebSocket conn) {
        if (conn == null) return null;
        return onlineUserObjects.get(conn);
    }

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
        if (conn != null) {
            User removedUser = onlineUserObjects.remove(conn);
            if (removedUser != null) {
                System.out.println("[ServerContext] Đã xóa đối tượng User khỏi RAM: " + removedUser.getUsername());
                broadcastOnlineUsersToAdmins(); // Tự động đồng bộ tới Admin khi có người ra
            }
        }
    }

    /**
     * Lấy danh sách toàn bộ đối tượng User đang trực tuyến dưới dạng List công khai
     */
    public List<User> getOnlineUserList() {
        return new ArrayList<>(onlineUserObjects.values());
    }

    public User getUserByConnObject(WebSocket conn) {
        return getUserCacheByConn(conn); // Điều hướng về hàm tối ưu
    }

    /**
     * Cập nhật trạng thái (status) trực tiếp trên RAM cho một User dựa theo Email.
     */
    public void updateUserStatusInRam(String email, String newStatus) {
        if (email == null || email.trim().isEmpty()) return;

        User user = getUserCacheByEmail(email);
        if (user != null) {
            user.setStatus(newStatus);
            System.out.println("[ServerContext] Đã cập nhật trạng thái của User [" + email + "] trên RAM thành: " + newStatus);
            broadcastOnlineUsersToAdmins(); // Đồng bộ ngay lập tức sang Admin Client
        }
    }

    /**
     * Xóa một User đang online dựa vào Email, dọn dẹp sạch sẽ ở CẢ 2 DANH SÁCH QUẢN LÝ
     */
    public void removeOnlineUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return;

        WebSocket connToRemove = onlineUsers.remove(email);

        if (connToRemove != null) {
            connToUserKey.remove(connToRemove);
            onlineUserObjects.remove(connToRemove);
            removeTikTokListener(connToRemove); // Dọn dẹp luôn các bộ lắng nghe TikTok nếu có
            System.out.println("[ServerContext] Đã xóa hoàn toàn User có email [" + email + "] khỏi các danh sách online.");

            // Phát tín hiệu cập nhật (Broadcast) đồng bộ danh sách mới về cho tất cả Admin
            broadcastOnlineUsersToAdmins();
        }
    }

    /**
     * Hàm Broadcast gửi danh sách toàn bộ người dùng đang online tới tất cả các kết nối là ADMIN (Chạy ASYNC)
     */
    public void broadcastOnlineUsersToAdmins() {
        asyncExecutor.submit(() -> {
            try {
                Response response = new Response(MessageType.GET_ONLINE_USERS_RESPONSE, "SUCCESS", "Cập nhật danh sách user online.");
                response.getData().put("userList", getOnlineUserList());
                String jsonResponse = gson.toJson(response);

                onlineUsers.forEach((email, conn) -> {
                    if (conn != null && conn.isOpen()) {
                        // Check quyền admin trực tiếp từ RAM Cache thay vì chọc DB
                        User cachedUser = onlineUserObjects.get(conn);
                        if (cachedUser != null && "ADMIN".equalsIgnoreCase(cachedUser.getRole())) {
                            conn.send(jsonResponse);
                            System.out.println("[AsyncAdminBroadcast] Đã đẩy danh sách Online Users tới Admin: " + email);
                        }
                    }
                });
            } catch (Exception e) {
                System.err.println("[AsyncAdminBroadcast] Lỗi khi xử lý gửi tin tới Admin: " + e.getMessage());
            }
        });
    }
    public WebSocket getConnByUser(String email) {
        if (email == null) return null;
        return onlineUsers.get(email); // O(1) cực nhanh
    }

    public Map<WebSocket, User> getOnlineUserObjects() {
        return this.onlineUserObjects;
    }
}