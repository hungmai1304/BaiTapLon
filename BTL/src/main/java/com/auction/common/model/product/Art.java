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
}