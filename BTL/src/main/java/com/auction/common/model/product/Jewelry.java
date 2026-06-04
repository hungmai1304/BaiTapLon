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

    @Override
    public String getSpecialDetails() {
        return "\n\n[Đặc tả Trang sức]"
                + "\n- Loại trang sức: " + (type != null && !type.isEmpty() ? type : "Chưa cập nhật")
                + "\n- Chất liệu: " + (material != null && !material.isEmpty() ? material : "Chưa cập nhật");
    }
}