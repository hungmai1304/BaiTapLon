package com.auction.common.model.auction;

import com.auction.common.model.product.Item;
import com.auction.common.model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int id;
    private Item item;
    private double startPrice;
    private double stepPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private User highestBidder;
    private String status;

    // 🚀 ĐỒ CỦA BACKEND (Anh em mình thêm)
    private List<BidTransaction> biddingHistory = new ArrayList<>();

    // 🚀 ĐỒ CỦA UI (Hải Anh thêm)
    private String leaderName;

    // Hải Anh thêm Constructor rỗng (để Gson dễ đọc dữ liệu)
    public Auction() {}

    public Auction(int id, Item item, double startPrice, double stepPrice, double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.id=id;
        this.item=item;
        this.startPrice=startPrice;
        this.stepPrice=stepPrice;
        this.currentPrice=currentPrice;
        this.startTime=startTime;
        this.endTime=endTime;
        this.highestBidder=null;
        this.status="PENDING";
        this.leaderName="Chưa có"; // Khởi tạo mặc định cho UI đỡ lỗi
    }

    // --- GETTER / SETTER CƠ BẢN ---
    public int getId() { return id; }
    public void setId(int id) { this.id = id;}
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
    public double getStepPrice() { return stepPrice; }
    public void setStepPrice(double stepPrice) { this.stepPrice = stepPrice; }
    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }
    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public User getHighestBidder() { return highestBidder; }
    public void setHighestBidder(User highestBidder) { this.highestBidder = highestBidder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    // --- GETTER/SETTER CHO LỊCH SỬ (CỦA ANH) ---
    public List<BidTransaction> getBiddingHistory() { return biddingHistory; }
    public void setBiddingHistory(List<BidTransaction> biddingHistory) { this.biddingHistory = biddingHistory; }

    // --- GETTER/SETTER CHO LEADER NAME (CỦA HẢI ANH) ---
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
}