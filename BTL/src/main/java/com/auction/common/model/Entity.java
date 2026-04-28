package com.auction.common.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Lớp cơ sở cho mọi đối tượng trong hệ thống.
 * Giúp quản lý ID và thời gian khởi tạo một cách thống nhất.
 */
public abstract class Entity {
    protected String id;
    protected LocalDateTime createdAt;

    // Constructor mặc định: Tự động gán giá trị khi một đối tượng con được tạo ra
    public Entity() {
        this.id = UUID.randomUUID().toString(); // Tạo một mã định danh duy nhất (Ví dụ: 550e8400-e29b...)
        this.createdAt = LocalDateTime.now();   // Lấy thời gian hiện tại của hệ thống
    }

    // Getter cho ID
    public String getId() {
        return id;
    }

    // Getter cho thời gian tạo
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // Setter (nếu sau này bạn cần gán ID từ Database về)
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "id='" + id + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}