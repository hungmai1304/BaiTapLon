package com.auction.client.utils;

import com.auction.common.model.product.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class ClientContext {
    private static ClientContext instance;
    private final ObservableList<Product> auctionProducts = FXCollections.observableArrayList();

    // Biến con trỏ hiện tại
    private int currentIndex = 0;

    private ClientContext() {}

    public static synchronized ClientContext getInstance() {
        if (instance == null) instance = new ClientContext();
        return instance;
    }

    public void setAuctionProducts(List<Product> products) {
        this.auctionProducts.setAll(products);
        this.currentIndex = 0; // Reset về đầu danh sách khi nhận list mới
    }

    public ObservableList<Product> getAuctionProducts() {
        return auctionProducts;
    }

    // Lấy sản phẩm hiện tại dựa trên con trỏ
    public Product getCurrentProduct() {
        if (auctionProducts.isEmpty()) return null;
        return auctionProducts.get(currentIndex);
    }

    // Logic di chuyển tới
    public boolean next() {
        if (currentIndex < auctionProducts.size() - 1) {
            currentIndex++;
            return true; // Di chuyển thành công
        }
        return false; // Đã chạm biên dưới
    }

    // Logic di chuyển lùi
    public boolean previous() {
        if (currentIndex > 0) {
            currentIndex--;
            return true; // Di chuyển thành công
        }
        return false; // Đã chạm biên trên
    }

    public int getCurrentIndex() { return currentIndex; }
}