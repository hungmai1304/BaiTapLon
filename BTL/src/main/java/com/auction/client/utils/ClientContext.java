package com.auction.client.utils;

import com.auction.common.model.auction.Auction;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class ClientContext {
    private static ClientContext instance;

    // Danh sách quản lý duy nhất: Toàn bộ phiên đấu giá đang hoạt động
    private final ObservableList<Auction> activeAuctions = FXCollections.observableArrayList();

    // Con trỏ (index) duy nhất để điều hướng (lướt Up/Down)
    private int currentIndex = 0;

    private ClientContext() {}

    public static synchronized ClientContext getInstance() {
        if (instance == null) {
            instance = new ClientContext();
        }
        return instance;
    }

    /**
     * Cập nhật danh sách đấu giá mới nhất từ Server
     */
    public void setAuctionList(List<Auction> auctions) {
        if (auctions == null) return;

        this.activeAuctions.setAll(auctions);

        // Kiểm tra an toàn cho con trỏ sau khi cập nhật list mới
        if (currentIndex >= activeAuctions.size()) {
            this.currentIndex = Math.max(0, activeAuctions.size() - 1);
        }
    }

    public ObservableList<Auction> getActiveAuctions() {
        return activeAuctions;
    }

    /**
     * Lấy phiên đấu giá tại vị trí con trỏ hiện tại
     */
    public Auction getCurrentAuction() {
        if (activeAuctions.isEmpty()) return null;

        // Đảm bảo index không bị out of bounds
        if (currentIndex < 0 || currentIndex >= activeAuctions.size()) {
            currentIndex = 0;
        }
        return activeAuctions.get(currentIndex);
    }

    /**
     * Di chuyển tới phiên đấu giá tiếp theo (Dùng cho nút Down)
     */
    public boolean nextAuction() {
        if (currentIndex < activeAuctions.size() - 1) {
            currentIndex++;
            return true;
        }
        return false;
    }

    /**
     * Di chuyển lùi lại phiên đấu giá trước (Dùng cho nút Up)
     */
    public boolean previousAuction() {
        if (currentIndex > 0) {
            currentIndex--;
            return true;
        }
        return false;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public void setCurrentIndex(int index) {
        if (index >= 0 && index < activeAuctions.size()) {
            this.currentIndex = index;
        }
    }

    /**
     * Xóa sạch dữ liệu khi logout hoặc đóng app
     */
    public void clear() {
        activeAuctions.clear();
        currentIndex = 0;
    }
}