package com.auction.common.model.product;

public class Vehicle extends Product {
    private String brand;      // Hãng xe (VD: Mercedes, Toyota, VinFast...)
    private int year;          // Năm sản xuất
    private double mileage;    // Số km đã đi (Odo)
    private String engineType; // Loại động cơ (Xăng, Dầu, Điện...)

    public Vehicle() {}

    public String getBrand() {
        return brand;
    }
    public void setBrand(String brand) {
        this.brand = brand;
    }
    public int getYear() {
        return year;
    }
    public void setYear(int year) {
        this.year = year;
    }
    public double getMileage() {
        return mileage;
    }
    public void setMileage(double mileage) {
        this.mileage = mileage;
    }
    public String getEngineType() {
        return engineType;
    }
    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    @Override
    public String getSpecialDetails() {
        return "\n\n[Đặc tả Phương tiện]"
                + "\n- Hãng xe: " + (brand != null && !brand.isEmpty() ? brand : "Chưa cập nhật")
                + "\n- Năm sản xuất: " + year
                + "\n- Số Km đã đi: " + mileage + " km"
                + "\n- Loại động cơ: " + (engineType != null && !engineType.isEmpty() ? engineType : "Chưa cập nhật");
    }
}