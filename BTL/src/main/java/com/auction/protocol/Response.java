package com.auction.protocol;

import java.io.Serializable;

public class Response implements Serializable {
    private static final long serialVersionUID = 1L;

    private MessageType type;    // Loại phản hồi (VD: LOGIN_RESPONSE)
    private boolean isSuccess;   // Server báo Thành công (true) hay Thất bại (false)
    private String message;      // Lời nhắn cụ thể (VD: "Sai mật khẩu", "Đặt giá thành công")
    private Object payload;      // Dữ liệu trả về (VD: Danh sách phiên đấu giá, Thông tin User)

    public Response(MessageType type, boolean isSuccess, String message, Object payload) {
        this.type = type;
        this.isSuccess = isSuccess;
        this.message = message;
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

    public boolean isSuccess() {
        return isSuccess;
    }

    public void setSuccess(boolean isSuccess) {
        this.isSuccess = isSuccess;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }
    
}