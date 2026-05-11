package com.auction.server.model;

import com.auction.server.AuctionWebSocketServer;
import com.auction.common.model.product.Product;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;
import java.util.Collections;

public class ServerContext {

    private static ServerContext instance;

    private AuctionWebSocketServer server;
    private Product currentProduct;

    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    // BỎ static ở đây: Singleton instance sẽ sở hữu danh sách này
    // Khởi tạo ngay lập tức để không bao giờ bị null
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

    public AuctionWebSocketServer getServer() { return server; }
    public Product getCurrentProduct() { return currentProduct; }
    public void setCurrentProduct(Product product) { this.currentProduct = product; }

    // Quản lý danh sách sản phẩm
    public List<Product> getProductList() {
        return productList;
    }

    public void addProduct(Product product) {
        if (product != null) {
            productList.add(product);
            System.out.println("[ServerContext] Đã thêm sản phẩm: " + product.getName());
        }
    }

    public void removeProduct(String productId) {
        productList.removeIf(p -> p.getId().equals(productId));
        System.out.println("[ServerContext] Đã xóa sản phẩm: " + productId);
    }

    public Product getProductById(String productId) {
        return productList.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    // --- Quản lý User ---
    public void addOnlineUser(String username, WebSocket conn) {
        onlineUsers.put(username, conn);
        System.out.println("[ServerContext] Khách hàng [" + username + "] đã đăng nhập!");
    }

    public void removeOnlineUser(String username) {
        onlineUsers.remove(username);
    }

    public boolean isUserOnline(String username) {
        return onlineUsers.containsKey(username);
    }

    public void removeUser(WebSocket conn) {
        if (conn == null) return;
        onlineUsers.values().remove(conn); // Cách xóa nhanh hơn theo value
    }
    // Lấy Username (hoặc Email) dựa trên kết nối WebSocket hiện tại
    public String getUserByConn(WebSocket conn) {
        if (conn == null) return null;

        return onlineUsers.entrySet().stream()
                .filter(entry -> entry.getValue().equals(conn))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    // Lấy WebSocket của một User dựa trên Username (đã có map onlineUsers)
    public WebSocket getConnByUser(String username) {
        return onlineUsers.get(username);
    }
}