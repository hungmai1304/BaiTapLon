package com.auction.common.model;

import java.io.Serializable;

public class Item implements Serializable {
    private static final long serialVersionUID = 1L;
    private int id;
    private String name;        // 1. Tên sản phẩm
    private String description; // 2. Mô tả sản phẩm
    private String imagePath;   // 3. Đường dẫn lưu ảnh của sản phẩm
    public Item(int id, String name, String description, String imagePath) {
        this.id=id;
        this.name=name;
        this.description=description;
        this.imagePath=imagePath;
    }
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}