package com.auction.common.model.product;

import com.auction.common.model.user.User;

import java.time.LocalDateTime;

public class Product extends Item {
    private String category;
    private double startPrice;
    private double currentPrice;
    private double stepPrice;

    private User owner;

    private LocalDateTime startTime;
    private LocalDateTime endTime;

    public Product() {}

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getStepPrice() {
        return stepPrice;
    }

    public void setStepPrice(double stepPrice) {
        this.stepPrice = stepPrice;
    }

    public User getOwner() {
        return owner;
    }

    public void setOwner(User owner) {
        this.owner = owner;
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
}