package com.auction.protocol;

import java.util.HashMap;
import java.util.Map;

public class Response {

    private String type;

    private String status;

    private String message;

    private Map<String, Object> data;

    // Constructor rỗng cho Gson
    public Response() {

        this.data = new HashMap<>();
    }

    public Response(String type,
                    String status,
                    String message) {

        this.type = type;

        this.status = status;

        this.message = message;

        this.data = new HashMap<>();
    }

    // =========================
    // GETTER
    // =========================

    public String getType() {

        return type;
    }

    public String getStatus() {

        return status;
    }

    public String getMessage() {

        return message;
    }

    public Map<String, Object> getData() {

        return data;
    }

    // =========================
    // SETTER
    // =========================

    public void setType(String type) {

        this.type = type;
    }

    public void setStatus(String status) {

        this.status = status;
    }

    public void setMessage(String message) {

        this.message = message;
    }

    public void setData(Map<String, Object> data) {

        this.data = data;
    }
}