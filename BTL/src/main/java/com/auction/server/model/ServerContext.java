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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class ServerContext {
    private static final Logger LOGGER = Logger.getLogger(ServerContext.class.getName());

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

    // Lưu danh sách auction online đang diễn ra (Key: Auction ID)
    private final Map<String, Auction> activeAuctionsMap = new ConcurrentHashMap<>();

    // TỐI ƯU: Bảng chỉ mục phụ kết nối chéo O(1) từ Product ID -> Auction ID (Chống nghẽn CPU cho Expire Bot)
    private final Map<String, String> productToAuctionIndex = new ConcurrentHashMap<>();

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

    public AuctionWebSocketServer getServer() {
        return server;
    }

    public List<Auction> getActiveAuctions() {
        return new ArrayList<>(activeAuctionsMap.values());
    }

    public void addAuction(Auction auction) {
        if (auction != null && auction.getId() != null) {
            activeAuctionsMap.put(auction.getId(), auction);

            // Đồng bộ xây dựng index phụ cho Product ID
            if (auction.getProduct() != null && auction.getProduct().getId() != null) {
                productToAuctionIndex.put(auction.getProduct().getId(), auction.getId());
            }

            LOGGER.info("[ServerContext] Đã thêm Phiên Đấu Giá (ID: " + auction.getId() + ") vào RAM.");
            broadcastAuctionUpdate();
        }
    }

    public void removeAuction(String auctionId) {
        if (auctionId == null) return;
        Auction removed = activeAuctionsMap.remove(auctionId);
        if (removed != null) {
            // Dọn dẹp index phụ để tránh rác RAM (Memory Leak)
            if (removed.getProduct() != null && removed.getProduct().getId() != null) {
                productToAuctionIndex.remove(removed.getProduct().getId());
            }
            LOGGER.info("[ServerContext] Đã xóa Phiên Đấu Giá (ID: " + auctionId + ") khỏi RAM.");
            broadcastAuctionUpdate();
        }
    }

    public void updateAuction(Auction updatedAuction) {
        if (updatedAuction == null || updatedAuction.getId() == null) return;

        activeAuctionsMap.put(updatedAuction.getId(), updatedAuction);

        // Cập nhật lại chỉ mục phụ nếu cần
        if (updatedAuction.getProduct() != null && updatedAuction.getProduct().getId() != null) {
            productToAuctionIndex.put(updatedAuction.getProduct().getId(), updatedAuction.getId());
        }

        LOGGER.info("[ServerContext] Đã cập nhật Auction ID: " + updatedAuction.getId());
        broadcastAuctionUpdate();
    }

    /**
     * ĐÃ TỐI ƯU TUYỆT ĐỐI O(1): Tìm nhanh phiên đấu giá từ mã sản phẩm thông qua Index phụ, không quét mảng.
     */
    public Auction getAuctionByProductId(String productId) {
        if (productId == null) return null;
        String auctionId = productToAuctionIndex.get(productId);
        return auctionId != null ? activeAuctionsMap.get(auctionId) : null;
    }

    public void removeAuctionByProductId(String productId) {
        if (productId == null) return;

        Auction targetAuction = getAuctionByProductId(productId);
        if (targetAuction != null) {
            activeAuctionsMap.remove(targetAuction.getId());
            productToAuctionIndex.remove(productId); // Xóa sạch cả 2 bản đồ
            System.out.println("[ServerContext] [ĐỒNG BỘ] Đã xóa Phiên Đấu Giá (ID: "
                    + targetAuction.getId() + ") của sản phẩm hết hạn (ID: " + productId + ") khỏi RAM.");
            broadcastAuctionUpdate();
        }
    }

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
     * SIÊU TỐI ƯU BROADCAST: Đẩy toàn bộ tác vụ nặng (chụp snapshot + dịch JSON + gửi I/O) xuống luồng ngầm.
     */
    private void broadcastAuctionUpdate() {
        if (tiktokListeners.isEmpty()) return;

        // Chụp nhanh snapshot của danh sách dữ liệu tại thời điểm gọi (Chỉ tốn O(1) sao chép reference mảng)
        final List<Auction> snapshot = new ArrayList<>(activeAuctionsMap.values());

        // Đẩy toàn bộ "cục tạ" nặng nề xuống ExecutorService ngầm
        asyncExecutor.submit(() -> {
            try {
                // Luồng ngầm tự chịu trách nhiệm dịch JSON, luồng nghiệp vụ chính đã được giải phóng tự do!
                Response response = new Response(MessageType.GET_ACTIVE_AUCTIONS_RESPONSE, "SUCCESS", "Cập nhật danh sách đấu giá.");
                response.getData().put("auctionList", snapshot);
                String json = gson.toJson(response);

                LOGGER.info("[AsyncBroadcast] Đang phát chuỗi JSON tới " + tiktokListeners.size() + " listeners từ luồng ngầm.");

                for (WebSocket conn : tiktokListeners) {
                    if (conn != null && conn.isOpen()) {
                        // Tiếp tục cô lập hành vi blocking ghi mạng của từng client thông qua CompletableFuture
                        CompletableFuture.runAsync(() -> {
                            try {
                                if (conn.isOpen()) {
                                    conn.send(json);
                                }
                            } catch (Exception ignored) {}
                        }, asyncExecutor);
                    }
                }
            } catch (Exception e) {
                LOGGER.severe("[AsyncBroadcast] Lỗi hệ thống khi xử lý phát loa đấu giá: " + e.getMessage());
            }
        });
    }

    public void addOnlineUser(String userId, WebSocket conn) {
        if (userId == null || conn == null) return;
        onlineUsers.put(userId, conn);
        connToUserKey.put(conn, userId);
        LOGGER.info("[ServerContext] User [" + userId + "] đã online!");
    }

    public void removeUser(WebSocket conn) {
        if (conn == null) return;

        String userId = connToUserKey.remove(conn);
        if (userId != null) {
            onlineUsers.remove(userId);
        } else {
            onlineUsers.entrySet().removeIf(entry -> entry.getValue().equals(conn));
        }
        removeOnlineUserObject(conn);
        removeTikTokListener(conn);
    }

    public String getUserByConn(WebSocket conn) {
        if (conn == null) return null;
        return connToUserKey.get(conn);
    }

    public Collection<WebSocket> getConnectedClients() {
        return onlineUsers.values();
    }

    public User getUserCacheByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;

        WebSocket conn = onlineUsers.get(email);
        if (conn != null) {
            return onlineUserObjects.get(conn);
        }

        return onlineUserObjects.values().stream()
                .filter(u -> u != null && email.equalsIgnoreCase(u.getEmail()))
                .findFirst()
                .orElse(null);
    }

    public User getUserCacheByConn(WebSocket conn) {
        if (conn == null) return null;
        return onlineUserObjects.get(conn);
    }

    public void addOnlineUserObject(WebSocket conn, User user) {
        if (conn != null && user != null) {
            onlineUserObjects.put(conn, user);
            LOGGER.info("[ServerContext] Lưu trữ thông tin đối tượng User: " + user.getUsername() + " | Trạng thái: " + user.getStatus());
            broadcastOnlineUsersToAdmins();
        }
    }

    public void removeOnlineUserObject(WebSocket conn) {
        if (conn != null) {
            User removedUser = onlineUserObjects.remove(conn);
            if (removedUser != null) {
                LOGGER.info("[ServerContext] Đã xóa đối tượng User khỏi RAM: " + removedUser.getUsername());
                broadcastOnlineUsersToAdmins();
            }
        }
    }

    public List<User> getOnlineUserList() {
        return new ArrayList<>(onlineUserObjects.values());
    }

    public User getUserByConnObject(WebSocket conn) {
        return getUserCacheByConn(conn);
    }

    public void updateUserStatusInRam(String email, String newStatus) {
        if (email == null || email.trim().isEmpty()) return;

        User user = getUserCacheByEmail(email);
        if (user != null) {
            user.setStatus(newStatus);
            LOGGER.info("[ServerContext] Đã cập nhật trạng thái của User [" + email + "] trên RAM thành: " + newStatus);
            broadcastOnlineUsersToAdmins();
        }
    }

    public void removeOnlineUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return;

        WebSocket connToRemove = onlineUsers.remove(email);

        if (connToRemove != null) {
            connToUserKey.remove(connToRemove);
            onlineUserObjects.remove(connToRemove);
            removeTikTokListener(connToRemove);
            LOGGER.info("[ServerContext] Đã xóa hoàn toàn User có email [" + email + "] khỏi các danh sách online.");
            broadcastOnlineUsersToAdmins();
        }
    }

    public void broadcastOnlineUsersToAdmins() {
        if (onlineUsers.isEmpty()) return;

        // Chụp nhanh snapshot danh sách user online
        final List<User> userListSnapshot = getOnlineUserList();

        asyncExecutor.submit(() -> {
            try {
                Response response = new Response(MessageType.GET_ONLINE_USERS_RESPONSE, "SUCCESS", "Cập nhật danh sách user online.");
                response.getData().put("userList", userListSnapshot);
                final String jsonResponse = gson.toJson(response);

                List<WebSocket> adminConnections = new ArrayList<>();
                onlineUserObjects.forEach((conn, user) -> {
                    if (user != null && "ADMIN".equalsIgnoreCase(user.getRole()) && conn.isOpen()) {
                        adminConnections.add(conn);
                    }
                });

                for (WebSocket adminConn : adminConnections) {
                    CompletableFuture.runAsync(() -> {
                        try {
                            if (adminConn.isOpen()) {
                                adminConn.send(jsonResponse);
                            }
                        } catch (Exception ignored) {}
                    }, asyncExecutor);
                }
            } catch (Exception e) {
                LOGGER.severe("[AsyncAdminBroadcast] Lỗi hệ thống khi gửi tin tới Admin: " + e.getMessage());
            }
        });
    }

    public WebSocket getConnByUser(String email) {
        if (email == null) return null;
        return onlineUsers.get(email);
    }

    public Map<WebSocket, User> getOnlineUserObjects() {
        return this.onlineUserObjects;
    }
}