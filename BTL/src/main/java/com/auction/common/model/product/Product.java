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
    private String imagePath;
    private String imageBase64;

    // --- HAI TRƯỜNG MỚI: Dùng Integer để có thể nhận giá trị null ---
    private Integer waitingMinutes = null;
    private Integer durationMinutes = null;

    public String getImageBase64() { return imageBase64; }
    public void setImageBase64(String imageBase64) { this.imageBase64 = imageBase64; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    private ProductStatus status = ProductStatus.AVAILABLE;

    public Product() {}

    // --- GETTER / SETTER CHO HAI TRƯỜNG MỚI ---
    public Integer getWaitingMinutes() { return waitingMinutes; }
    public void setWaitingMinutes(Integer waitingMinutes) { this.waitingMinutes = waitingMinutes; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    // --- các getter/setter cũ giữ nguyên hoàn toàn ---
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

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

    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
}