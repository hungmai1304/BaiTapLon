package com.auction.common.model.user;


import com.auction.common.model.base.BaseEntity;

public abstract class User extends BaseEntity {
    private String username;
    private String password;

    public User() {}

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}