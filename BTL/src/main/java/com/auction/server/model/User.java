package com.auction.server.model;

import java.sql.Timestamp;

public class User {
    private String id;
    private String email;
    private String password;
    private String name;
    private Timestamp timeCreated;
    private String shopName; // Có thể null nếu là Bidder
    private String role;     // BIDDER hoặc SELLER

    public User() {
    }

    public User(String id, String email, String password, String name, Timestamp timeCreated, String shopName, String role) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.name = name;
        this.timeCreated = timeCreated;
        this.shopName = shopName;
        this.role = role;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Timestamp getTimeCreated() {
        return timeCreated;
    }

    public void setTimeCreated(Timestamp timeCreated) {
        this.timeCreated = timeCreated;
    }

    public String getShopName() {
        return shopName;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}