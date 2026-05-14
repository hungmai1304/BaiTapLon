package com.auction.common.model.auction;

import com.auction.common.model.product.Item;
import com.auction.common.model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Auction {
    private int id;
    private Item item;                  // Món hàng được đem ra đấu giá
    private double startPrice;          // Giá khởi điểm
    private double stepPrice;           // Bước giá (yêu cầu của bài)
    private double currentPrice;        // Giá hiện tại
    private LocalDateTime startTime;    // Thời gian bắt đầu
    private LocalDateTime endTime;      // Thời gian kết thúc
    private User highestBidder;         // Người chơi đang dẫn đầu
    private String status;              // Trạng thái: "PENDING", "ACTIVE", "COMPLETED"
    // Ngăn chứa các lượt trả giá
    private List<BidTransaction> biddingHistory = new ArrayList<>();

    public Auction(int id, Item item, double startPrice, double stepPrice, double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.id=id;
        this.item=item;
        this.startPrice=startPrice;
        this.stepPrice=stepPrice;
        this.currentPrice=currentPrice;
        this.startTime=startTime;
        this.endTime=endTime;
        this.highestBidder=null;      // Chưa có ai đấu giá
        this.status="PENDING";        // Mới tạo thì đang chờ tới giờ
    }
    public int getId() { return id; }
    public void setId(int id) { this.id = id;}
    public Item getItem() { return item; }
    public void setItem(Item item) { this.item = item; }
    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }
    public double getStepPrice() { return stepPrice; }
    public void setStepPrice(double stepPrice) { this.stepPrice = stepPrice; }
    public double getCurrentPrice() {
        return currentPrice;
    }
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }
    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }
    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
    public User getHighestBidder() {
        return highestBidder;
    }
    public void setHighestBidder(User highestBidder) {
        this.highestBidder = highestBidder;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public List<BidTransaction> getBiddingHistory() {
        return biddingHistory;
    }
    public void setBiddingHistory(List<BidTransaction> biddingHistory) {
        this.biddingHistory = biddingHistory;
    }

    
}
