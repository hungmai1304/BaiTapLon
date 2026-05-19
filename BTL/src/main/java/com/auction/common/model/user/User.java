package com.auction.common.model.user;

import com.auction.common.model.base.BaseEntity;
import java.time.LocalDateTime;

public class User extends BaseEntity {
    private String username;
    private String password;
    private String email;

    // Trường role mới thêm của bạn
    private String role;

    // Các thông tin thời gian chung
    private double balance;

    // VIẾT THÊM: Thêm trường status để quản lý trạng thái tài khoản (ACTIVE, BANNED, v.v.)
    private String status;

    public User() {}

    // --- Getter và Setter cho các trường cơ bản ---
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

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

    // --- BỔ SUNG: GETTER VÀ SETTER CHO ROLE TẠI ĐÂY ---
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // --- THÊM GETTER VÀ SETTER CHO BALANCE Ở ĐÂY ---
    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    // --- VIẾT THÊM: GETTER VÀ SETTER CHO STATUS TẠI ĐÂY ---
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}