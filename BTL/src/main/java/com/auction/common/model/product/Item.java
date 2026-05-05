package com.auction.common.model.product;

import com.auction.common.model.base.BaseEntity;

import java.io.Serializable;


public abstract class Item extends BaseEntity {
    private String name;

    public Item() {}

    public Item(String id, String name) {
        super.setId(id);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}