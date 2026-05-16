package com.auction.protocol;

import java.util.HashMap;
import java.util.Map;

public class Request {

    protected String type;
    protected Map<String, Object> data;

    public Request() {
        this.data = new HashMap<>();
    }

    // THÊM MỚI - Constructor tiện lợi
    public Request(String type) {
        this.type = type;
        this.data = new HashMap<>();
    }

    // THÊM MỚI - Thêm filter vào data dễ dàng
    public Request addData(String key, Object value) {
        this.data.put(key, value);
        return this; // cho phép chain: new Request(...).addData(...).addData(...)
    }


    public String getType() { return type; }
    public Map<String, Object> getData() { return data; }
}