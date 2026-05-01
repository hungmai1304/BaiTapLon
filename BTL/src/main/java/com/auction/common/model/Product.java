package com.auction.common.model;



import java.io.Serializable;

public class Product implements Serializable {
    // serialVersionUID giúp định danh phiên bản của class khi truyền qua mạng
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private double currentPrice;
    private double stepPrice;
    private String description;

    // Constructor không tham số (cần thiết cho một số thư viện và kế thừa)
    public Product() {
    }

    // Constructor đầy đủ tham số để khởi tạo nhanh
    public Product(String id, String name, double currentPrice, double stepPrice) {
        this.id = id;
        this.name = name;
        this.currentPrice = currentPrice;
        this.stepPrice = stepPrice;
    }

    // --- Getters và Setters ---

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Ghi đè toString để dễ dàng debug khi in ra console
    @Override
    public String toString() {
        return "Product{" +
                "name='" + name + '\'' +
                ", currentPrice=" + currentPrice +
                ", stepPrice=" + stepPrice +
                '}';
    }
}