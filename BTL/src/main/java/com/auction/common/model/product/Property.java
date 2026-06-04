package com.auction.common.model.product;

public class Property extends Product {
    // 1. Khai báo các thuộc tính riêng của Bất động sản
    private String address; // Địa chỉ
    private double area;    // Diện tích (m2)

    // 2. Constructor mặc định (rất quan trọng để Gson có thể parse được dữ liệu)
    public Property() {
        super(); // Gọi constructor của class cha (Product)

        // LƯU Ý TỪ LỖI LÚC TRƯỚC:
        // Nếu trong class cha Product của anh có một biến để phân biệt loại (ví dụ: String type;)
        // thì anh BẮT BUỘC phải gán giá trị cho nó ở đây để nó map đúng với GsonUtil.
        // Mở comment dòng dưới nếu class Product của anh dùng biến type:
        // this.type = "Property";
    }

    // 3. Getter và Setter
    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    // 4. Override hàm in thông tin chi tiết (Giữ đúng format của Fashion)
    @Override
    public String getSpecialDetails() {
        // Xử lý null cho address và kiểm tra area > 0
        return "\n\n[Đặc tả Bất động sản]"
                + "\n- Địa chỉ: " + (address != null && !address.trim().isEmpty() ? address : "Chưa cập nhật")
                + "\n- Diện tích: " + (area > 0 ? area + " m2" : "Chưa cập nhật");
    }
}