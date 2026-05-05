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
}