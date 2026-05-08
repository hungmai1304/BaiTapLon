package com.auction.server.model;

import com.auction.server.AuctionWebSocketServer;
import com.auction.common.model.product.Product;
import org.java_websocket.WebSocket; // Thêm thư viện này vào để quản lý kết nối

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ServerContext {

    // 1. CHUẨN SINGLETON: Đảm bảo chỉ có 1 bộ nhớ RAM duy nhất
    private static ServerContext instance;

    private AuctionWebSocketServer server;
    private Product currentProduct;

    // 3. THÊM BỘ NHỚ AN TOÀN LUỒNG 
    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    // Khóa cửa, không cho ai dùng lệnh "new" bừa bãi
    private ServerContext() {}

    // Cổng duy nhất để lấy bộ nhớ RAM ra dùng
    public static synchronized ServerContext getInstance() {
        if (instance == null) {
            instance = new ServerContext();
        }
        return instance;
    }

    // Hàm khởi tạo dữ liệu ban đầu
    public void initData(AuctionWebSocketServer server, Product currentProduct) {
        this.server = server;
        this.currentProduct = currentProduct;
    }

    public AuctionWebSocketServer getServer() { return server; }
    public Product getCurrentProduct() { return currentProduct; }

    // --- Các hàm quản lý User (chuẩn bị cho LoginHandler) ---
    public void addOnlineUser(String username, WebSocket conn) {
        onlineUsers.put(username, conn);
        System.out.println("[ServerContext] Khách hàng [" + username + "] đã đăng nhập!");
    }

    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
        System.out.println("[ServerContext] Khách hàng [" + username + "] đã thoát!");
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public void removeUser(WebSocket conn) {
        if (conn == null) return;

        String disconnectedUser = null;

        // Tim trong kho client
        for (Map.Entry<String, WebSocket> entry : onlineUsers.entrySet()) {
            if (entry.getValue().equals(conn)) {
                disconnectedUser = entry.getKey();
                break; // Tìm thấy rồi thì dừng lại cho đỡ tốn CPU
            }
        }

        // Xoa khoi danh sach neu thoat
        if (disconnectedUser != null) {
            onlineUsers.remove(disconnectedUser);
            System.out.println("[ServerContext] 🧹 Đã dọn dẹp Session của user vừa thoát: " + disconnectedUser);
        }
    }
}