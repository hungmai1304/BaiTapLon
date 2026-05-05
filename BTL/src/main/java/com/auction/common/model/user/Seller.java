package com.auction.common.model.user;

public class Seller extends User {
    private String shopName;

    public Seller() {}

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }
}