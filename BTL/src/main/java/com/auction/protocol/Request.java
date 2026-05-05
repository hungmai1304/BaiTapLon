package com.auction.protocol;

import java.util.Map;

public class Request {

    protected String type;
    protected Map<String, Object> data;

    // Getters
    public String getType() {
        return type;
    }

    public Map<String, Object> getData() {
        return data;
    }
}