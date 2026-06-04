package com.auction.common.model.product;


public class Art extends Product {
    private String artistName;
    private int age; // độ cổ

    public Art() {}

    public String getArtistName() {
        return artistName;
    }

    public void setArtistName(String artistName) {
        this.artistName = artistName;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String getSpecialDetails() {
        return "\n\n[Đặc tả Nghệ thuật]"
                + "\n- Tác giả: " + (getArtistName() != null && !getArtistName().isEmpty() ? getArtistName() : "Khuyết danh")
                + "\n- Tuổi đời: " + getAge() + " năm";
    }
}