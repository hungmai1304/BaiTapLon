package com.auction.protocol;

public class MessageType {
    // Các tín hiệu liên quan đến Đăng nhập
    public static final String LOGIN_REQUEST = "LOGIN_REQUEST";
    public static final String LOGIN_RESPONSE = "LOGIN_RESPONSE";


    public static final String REGISTER_REQUEST = "REGISTER_REQUEST";
    public static final String REGISTER_RESPONSE = "REGISTER_RESPONSE";

    // Yêu cầu lấy danh sách hàng
    public static final String GET_PRODUCTS_REQUEST = "GET_PRODUCTS_REQUEST";
    // Server trả về danh sách hàng
    public static final String GET_PRODUCTS_RESPONSE = "GET_PRODUCTS_RESPONSE";

    // (Sau này làm tính năng gì thì cứ khai báo thêm vào đây)
}