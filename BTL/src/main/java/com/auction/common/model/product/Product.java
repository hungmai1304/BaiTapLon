package com.auction.common.model.product;

import com.auction.common.model.user.User;
import java.time.LocalDateTime;
import com.auction.common.model.product.ProductStatus;
public class Product extends Item {
    private String category;
    private double startPrice;
    private double currentPrice;
    private double stepPrice;
    private String description;
    private User owner;

    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime timeCreated;

    // ✅ THÊM MỚI
    private ProductStatus status = ProductStatus.AVAILABLE;
    // mặc định đầu tiên là chờ sản phẩm
//    private String currentAuctionId = null; // khoi tao chua dau gia lan nao

    public Product() {}

    // --- getter/setter
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getStartPrice() { return startPrice; }
    public void setStartPrice(double startPrice) { this.startPrice = startPrice; }

    public double getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(double currentPrice) { this.currentPrice = currentPrice; }

    public double getStepPrice() { return stepPrice; }
    public void setStepPrice(double stepPrice) { this.stepPrice = stepPrice; }

    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public LocalDateTime getTimeCreated() { return timeCreated; }
    public void setTimeCreated(LocalDateTime timeCreated) { this.timeCreated = timeCreated; }
    // ✅ THÊM MỚI - getter/setter cho status và auctionId
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }

//    public String getCurrentAuctionId() { return currentAuctionId; }
//    public void setCurrentAuctionId(String id) { this.currentAuctionId = id; }
}