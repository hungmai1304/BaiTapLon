package com.auction.server.model;

import com.auction.server.AuctionWebSocketServer;
import com.auction.common.model.product.Product;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.ArrayList;

public class ServerContext {

    private static ServerContext instance;

    private AuctionWebSocketServer server;
    private Product currentProduct;

    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    //  THÊM MỚI - Danh sách toàn bộ sản phẩm trong hệ thống
    private final List<Product> productList = new ArrayList<>();

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

    //  THÊM MỚI - Quản lý danh sách sản phẩm
    public List<Product> getProductList() {
        return productList;
    }

    // Thêm 1 sản phẩm vào hệ thống
    public void addProduct(Product product) {
        productList.add(product);
        System.out.println("[ServerContext] ✅ Đã thêm sản phẩm: " + product.getName());
    }

    // Xóa 1 sản phẩm khỏi hệ thống
    public void removeProduct(String productId) {
        productList.removeIf(p -> p.getId().equals(productId));
        System.out.println("[ServerContext] ❌ Đã xóa sản phẩm: " + productId);
    }

    // Lấy 1 sản phẩm theo ID
    public Product getProductById(String productId) {
        return productList.stream()
                .filter(p -> p.getId().equals(productId))
                .findFirst()
                .orElse(null);
    }

    // --- Quản lý User
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

        for (Map.Entry<String, WebSocket> entry : onlineUsers.entrySet()) {
            if (entry.getValue().equals(conn)) {
                disconnectedUser = entry.getKey();
                break;
            }
        }

        if (disconnectedUser != null) {
            onlineUsers.remove(disconnectedUser);
            System.out.println("[ServerContext] 🧹 Đã dọn dẹp Session của user vừa thoát: " + disconnectedUser);
        }
    }
}