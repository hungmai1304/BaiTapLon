package com.auction.server.model;

import com.auction.server.AuctionWebSocketServer;
import com.auction.common.model.product.Product;
import com.auction.common.model.auction.Auction;
import org.java_websocket.WebSocket;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ServerContext {

    private static ServerContext instance;
    private AuctionWebSocketServer server;
    private Product currentProduct;

    // Quản lý User Online: Key là UserID hoặc Username, Value là kết nối WebSocket
    private final Map<String, WebSocket> onlineUsers = new ConcurrentHashMap<>();

    private final List<Auction> activeAuctions = Collections.synchronizedList(new ArrayList<>());

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

    // Hàm mày vừa viết, sửa lại logic check cho chắc chắn
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

    // 🚀 BỔ SUNG MỚI: QUẢN LÝ PHIÊN ĐẤU GIÁ (AUCTION) TRÊN RAM

    // Lấy toàn bộ danh sách phiên đấu giá
    public List<Auction> getActiveAuctions() {
        return activeAuctions;
    }

    // Thêm một phiên đấu giá mới vào RAM khi ai đó bấm "Lên sàn"
    public void addAuction(Auction auction) {
        if (auction != null) {
            activeAuctions.add(auction);
            System.out.println("[ServerContext] Đã thêm Phiên Đấu Giá (ID: " + auction.getId() + ") vào RAM.");
        }
    }

    // Xóa phiên đấu giá (khi kết thúc 10 phút)
    public void removeAuction(int auctionId) {
        activeAuctions.removeIf(a -> a.getId() == auctionId);
        System.out.println("[ServerContext] Đã xóa Phiên Đấu Giá (ID: " + auctionId + ") khỏi RAM.");
    }

    // Lấy thông tin 1 phiên đấu giá theo ID phiên
    public Auction getAuctionById(int auctionId) {
        synchronized (activeAuctions) {
            return activeAuctions.stream()
                    .filter(a -> a.getId() == auctionId)
                    .findFirst()
                    .orElse(null);
        }
    }

    // Cập nhật giá tiền hoặc người dẫn đầu mới vào RAM
    public void updateAuction(Auction updatedAuction) {
        if (updatedAuction == null) return;
        synchronized (activeAuctions) {
            for (int i = 0; i < activeAuctions.size(); i++) {
                if (activeAuctions.get(i).getId() == updatedAuction.getId()) {
                    activeAuctions.set(i, updatedAuction);
                    System.out.println("[ServerContext] Đã cập nhật RAM cho Phiên Đấu Giá ID: " + updatedAuction.getId());
                    return;
                }
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