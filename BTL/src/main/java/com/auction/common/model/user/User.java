package com.auction.common.model.user;

import com.auction.common.model.base.BaseEntity;

// Đã bỏ abstract để có thể khởi tạo: new User()
public class User extends BaseEntity {
    private String username;
    private String password;
    private String email;
    private String role;
    // Thêm trường email

    public User() {}

    // --- Getter và Setter cho Email ---
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    // --- Getter và Setter cũ ---
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