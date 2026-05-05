package com.auction.common.model.product;


public class Jewelry extends Product {
    private String material;
    private String type;

    public Jewelry() {}

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}