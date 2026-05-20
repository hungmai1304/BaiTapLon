package com.auction.common.model.auction;

import com.auction.common.model.base.BaseEntity;
import com.auction.common.model.product.Product;
import com.auction.common.model.user.User;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
// xin lỗi tất cả anh em vì đã k cho nó kế thừa BaseEntity ngay từ đầu
public class Auction extends BaseEntity {

    private Product product;
    private double startPrice;
    private double stepPrice;
    private double currentPrice;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private User highestBidder;
    private String status;

    // PHỤC VỤ BACKEND
    private List<BidTransaction> biddingHistory = new ArrayList<>();

    // PHỤC VỤ UI
    private String leaderName;

    // Constructor rỗng cho Gson dễ đọc dữ liệu
    public Auction() {}

    // Sửa lại cú pháp nhận vào và gán cho biến product
    public Auction( Product product, double startPrice, double stepPrice, double currentPrice, LocalDateTime startTime, LocalDateTime endTime) {

        this.product = product;
        this.startPrice = startPrice;
        this.stepPrice = stepPrice;
        this.currentPrice = currentPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.highestBidder = null;
        this.status = "PENDING";
        this.leaderName = "Chưa có"; // Khởi tạo mặc định cho UI đỡ lỗi
    }

    // --- GETTER / SETTER CƠ BẢN ---


    // Đã chuyển toàn bộ sang Product thay vì Item cũ
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }

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

    // --- GETTER/SETTER CHO LỊCH SỬ (BACKEND) ---
    public List<BidTransaction> getBiddingHistory() { return biddingHistory; }
    public void setBiddingHistory(List<BidTransaction> biddingHistory) { this.biddingHistory = biddingHistory; }

    // --- GETTER/SETTER CHO LEADER NAME (UI) ---
    public String getLeaderName() { return leaderName; }
    public void setLeaderName(String leaderName) { this.leaderName = leaderName; }
}