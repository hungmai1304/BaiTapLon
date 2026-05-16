package com.auction.client.utils;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class ClientContext {
    private static ClientContext instance;

    private final ObservableList<Auction> activeAuctions = FXCollections.observableArrayList();
    private final ObservableList<Product> shopProducts = FXCollections.observableArrayList();
    private int currentIndex = 0;

    private ClientContext() {}

    public static synchronized ClientContext getInstance() {
        if (instance == null) {
            instance = new ClientContext();
        }
        return instance;
    }

    // --- QUẢN LÝ SHOP PRODUCTS ---

    public void setShopProducts(List<Product> products) {
        if (products != null) {
            this.shopProducts.setAll(products);
        }
    }

    public ObservableList<Product> getShopProducts() {
        return shopProducts;
    }

    public int getShopProductCount() {
        return shopProducts.size();
    }

    public void addProductToShop(Product product) {
        if (product != null) {
            this.shopProducts.add(product);
        }
    }

    // --- QUẢN LÝ AUCTIONS ---

    public void setAuctionList(List<Auction> auctions) {
        if (auctions == null) return;
        this.activeAuctions.setAll(auctions);
        if (currentIndex >= activeAuctions.size()) {
            this.currentIndex = Math.max(0, activeAuctions.size() - 1);
        }
    }

    public Auction getCurrentAuction() {
        if (activeAuctions.isEmpty()) return null;
        if (currentIndex < 0 || currentIndex >= activeAuctions.size()) {
            currentIndex = 0;
        }
        return activeAuctions.get(currentIndex);
    }

    public boolean nextAuction() {
        if (currentIndex < activeAuctions.size() - 1) {
            currentIndex++;
            return true;
        }
        return false;
    }

    public boolean previousAuction() {
        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }
        return false;
    }

    public void clear() {
        activeAuctions.clear();
        shopProducts.clear();
        currentIndex = 0;
    }

    public ObservableList<Auction> getActiveAuctions() { return activeAuctions; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int index) { this.currentIndex = index; }
}