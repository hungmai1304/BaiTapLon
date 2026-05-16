package com.auction.client.utils;

import com.auction.client.controller.SomeGlobal;
import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class ClientContext {
    private static ClientContext instance;

    private final ObservableList<Auction> activeAuctions = FXCollections.observableArrayList();
    private final ObservableList<Product> shopProducts = FXCollections.observableArrayList();
    private int currentIndex = 0;
    private final DoubleProperty userBalance = new SimpleDoubleProperty(0.0);

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
        userBalance.set(0.0);
    }
    // --- QUẢN LÝ USER BALANCE ---

    /**
     * Cập nhật số dư mới vào ClientContext để UI tự động thay đổi,
     * đồng thời đồng bộ luôn vào đối tượng User hiện tại trong SomeGlobal.
     * * @param balance Số dư mới cần cập nhật
     */
    public void setUserBalance(double balance) {
        // 1. Cập nhật cho object User trước
        if (SomeGlobal.getCurrentUser() != null) {
            SomeGlobal.getCurrentUser().setBalance(balance);
        }

        // 2. Cập nhật cho Property trên luồng JavaFX để kích hoạt các UI đã bind tự nhảy số
        if (javafx.application.Platform.isFxApplicationThread()) {
            this.userBalance.set(balance);
        } else {
            javafx.application.Platform.runLater(() -> this.userBalance.set(balance));
        }
    }

    /**
     * Lấy giá trị số dư hiện tại dưới dạng double
     */
    public double getUserBalance() {
        return userBalance.get();
    }

    /**
     * Lấy đối tượng Property để thực hiện bind dữ liệu lên các thành phần UI (Label, Text,...)
     */
    public DoubleProperty userBalanceProperty() {
        return userBalance;
    }

    public void updateBalance(double newBalance) {
        // 1. Cập nhật cho Property để update UI
        this.setUserBalance(newBalance);

        // 2. Tự động cập nhật cho User object nếu có dữ liệu
        if (SomeGlobal.getCurrentUser() != null) {
            SomeGlobal.getCurrentUser().setBalance(newBalance);
        }
    }

    public ObservableList<Auction> getActiveAuctions() { return activeAuctions; }
    public int getCurrentIndex() { return currentIndex; }
    public void setCurrentIndex(int index) { this.currentIndex = index; }
}