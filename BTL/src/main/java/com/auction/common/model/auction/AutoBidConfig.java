package com.auction.common.model.auction;

import com.auction.common.model.base.BaseEntity;
import java.time.LocalDateTime;
import java.util.UUID;

public class AutoBidConfig extends BaseEntity {
    private String email;
    private double maxPrice;
    private double stepPrice;

    public AutoBidConfig() {
        super();
    }

    public AutoBidConfig(String email, double maxPrice, double stepPrice) {
        // Tự động sinh ID ngẫu nhiên và ghi nhận thời gian cài Bot từ class cha (BaseEntity)
        super(UUID.randomUUID().toString(), LocalDateTime.now());

        this.email = email;
        this.maxPrice = maxPrice;
        this.stepPrice = stepPrice;
    }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public double getMaxPrice() { return maxPrice; }
    public void setMaxPrice(double maxPrice) { this.maxPrice = maxPrice; }

    public double getStepPrice() { return stepPrice; }
    public void setStepPrice(double stepPrice) { this.stepPrice = stepPrice; }
}