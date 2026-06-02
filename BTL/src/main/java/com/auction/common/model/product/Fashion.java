package com.auction.common.model.product;

public class Fashion extends Product {
    private String size;
    private String material;

    public Fashion() {}

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    @Override
    public String getSpecialDetails() {
        return "\n\n[Đặc tả Thời trang]"
                + "\n- Kích cỡ: " + (size != null ? size : "Chưa cập nhật")
                + "\n- Chất liệu: " + (material != null ? material : "Chưa cập nhật");
    }
}