package com.auction.common.model.product;


public class Electronics extends Product {
    private String origin;
    private String condition; // độ mới

    public Electronics() {}

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    @Override
    public String getSpecialDetails() {
        return "\n\n[Đặc tả Thiết bị điện tử]"
                + "\n- Xuất xứ: " + (origin != null && !origin.isEmpty() ? origin : "Chưa cập nhật")
                + "\n- Tình trạng: " + (condition != null && !condition.isEmpty() ? condition : "Chưa cập nhật");
    }

}