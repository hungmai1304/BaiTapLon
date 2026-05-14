package com.auction.server.model;

import com.auction.server.AuctionWebSocketServer;
import com.auction.common.model.product.Product;
import org.java_websocket.WebSocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerContext {

    private static ServerContext instance;
    private AuctionWebSocketServer server;
    private Product currentProduct;

    // Quản lý User Online: Key là UserID hoặc Username, Value là kết nối WebSocket
    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    // Danh sách sản phẩm trên RAM (Dùng synchronizedList để an toàn khi nhiều Handler cùng đọc/ghi)
    private final List<Product> productList = Collections.synchronizedList(new ArrayList<>());

    private ServerContext() {}

    public static synchronized ServerContext getInstance() {
        if (instance == null) {
            instance = new ServerContext();
        }
        return instance;
    }

    public void initData(AuctionWebSocketServer server, Product currentProduct) {
        this.server = server;
        this.currentProduct = currentProduct;
    }

    // --- Getter/Setter cơ bản ---
    public AuctionWebSocketServer getServer() { return server; }
    public Product getCurrentProduct() { return currentProduct; }
    public void setCurrentProduct(Product product) { this.currentProduct = product; }

    // --- Quản lý danh sách sản phẩm (RAM) ---
    public List<Product> getProductList() {
        return productList;
    }

    public void addProduct(Product product) {
        if (product != null) {
            productList.add(product);
            System.out.println("[ServerContext] Đã thêm sản phẩm vào RAM: " + product.getName());
        }
    }

    public void removeProduct(String productId) {
        productList.removeIf(p -> p.getId().equals(productId));
        System.out.println("[ServerContext] Đã xóa sản phẩm khỏi RAM: " + productId);
    }

    public Product getProductById(String productId) {
        synchronized (productList) {
            return productList.stream()
                    .filter(p -> p.getId().equals(productId))
                    .findFirst()
                    .orElse(null);
        }
    }

    // Hàm mày vừa viết, tao sửa lại logic check cho chắc chắn
    public void updateProduct(Product updatedProduct) {
        if (updatedProduct == null || updatedProduct.getId() == null) return;

        synchronized (productList) {
            boolean found = false;
            for (int i = 0; i < productList.size(); i++) {
                if (productList.get(i).getId().equals(updatedProduct.getId())) {
                    productList.set(i, updatedProduct);
                    System.out.println("[ServerContext] Đã cập nhật RAM cho SP: " + updatedProduct.getName());
                    found = true;
                    break;
                }
            }
            // Nếu không tìm thấy trong danh sách hiện tại thì mới thêm mới
            if (!found) {
                addProduct(updatedProduct);
            }
        }
    }

    // --- Quản lý User (WebSocket) ---
    public void addOnlineUser(String userId, WebSocket conn) {
        onlineUsers.put(userId, conn);
        System.out.println("[ServerContext] User [" + userId + "] đã kết nối!");
    }

    public void removeUser(WebSocket conn) {
        if (conn == null) return;
        // Xóa an toàn bằng removeIf dựa trên giá trị (WebSocket)
        onlineUsers.entrySet().removeIf(entry -> entry.getValue().equals(conn));
    }

    public String getUserByConn(WebSocket conn) {
        if (conn == null) return null;
        return onlineUsers.entrySet().stream()
                .filter(entry -> entry.getValue().equals(conn))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    public WebSocket getConnByUser(String userId) {
        return onlineUsers.get(userId);
    }

    public Collection<WebSocket> getConnectedClients() {
        return onlineUsers.values();
    }
}