package com.auction.common.model.product;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;

    // Thuộc tính riêng của xe cộ
    private String engineType; // Loại động cơ (Ví dụ: Xăng, Điện, Xăng lai điện...)

    public Vehicle() {}

    public Vehicle(int id, String name, double currentPrice, String description, String engineType) {
        super(id, name, currentPrice, description);
        this.engineType = engineType;
    }

    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    @Override
    public String getItemType() {
        return "Phuong Tien Giao Thong";
    }
}