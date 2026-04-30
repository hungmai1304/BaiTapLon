package com.auction.common.model;


/**
 * Admin kế thừa từ User.
 * Chỉ bao gồm ID, thời gian tạo, tên và mật khẩu.
 */
public class Admin extends User {

    public Admin() {
        super();
    }

    public Admin(String name, String password) {
        super(name, password); // đặt tên và mật khẩu cho admin
    }

    @Override
    public String toString() {
        return "Admin{" +
                "id='" + getId() + '\'' +
                ", name='" + getName() + '\'' +
                ", createdAt=" + getCreatedAt() +
                '}';
    }
}