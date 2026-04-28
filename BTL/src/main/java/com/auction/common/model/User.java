package com.auction.common.model;


/**
 * Lớp User kế thừa từ Entity.
 * Đã có sẵn id và createdAt từ lớp cha.
 */
public class User extends Entity {
    private String name;
    private String password;

    // Constructor mặc định (cần thiết cho một số thư viện như Jackson/Gson)
    public User() {
        super();
    }

    // Constructor có tham số
    public User(String name, String password) {
        super(); // Gọi constructor của Entity để khởi tạo ID và thời gian tạo
        this.name = name;
        this.password = password;
    }

    // Getter và Setter cho Name
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // Getter và Setter cho Password
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public String toString() {
        return "User{" +
                "id='" + getId() + '\'' + // Lấy ID từ lớp cha Entity
                ", name='" + name + '\'' +
                ", createdAt=" + getCreatedAt() + // Lấy thời gian từ lớp cha Entity
                '}';
    }
}