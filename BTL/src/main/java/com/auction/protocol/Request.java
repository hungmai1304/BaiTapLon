package com.auction.protocol;

import java.io.Serializable;

public class Request implements Serializable {
    private static final long serialVersionUID=1L;
    private MessageType type;
    private Object payload; 
    public Request(MessageType type) {
        this.type = type; 
        this.payload = null;
    }
    public Request(MessageType type, Object payload) {
        this.type = type;
        this.payload = payload;
    }
    public static long getSerialversionuid() {
        return serialVersionUID;
    }
    public MessageType getType() {
        return type;
    }
    public void setType(MessageType type) {
        this.type = type;
    }
    public Object getPayload() {
        return payload;
    }
    public void setPayload(Object payload) {
        this.payload = payload;
    }
    
}