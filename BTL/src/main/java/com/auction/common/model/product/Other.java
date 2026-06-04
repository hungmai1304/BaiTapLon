package com.auction.common.model.product;

public class Other extends Product {

    public Other() {}
    
    @Override
    public String getSpecialDetails() {
        return "\n\n[Đặc tả: Khác]"
                + "\n- Sản phẩm này không thuộc các danh mục tiêu chuẩn.";
    }
}