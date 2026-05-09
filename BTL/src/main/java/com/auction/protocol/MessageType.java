package com.auction.protocol;

public class MessageType {
    // Các tín hiệu liên quan đến Đăng nhập
    public static final String LOGIN_REQUEST = "LOGIN_REQUEST";
    public static final String LOGIN_RESPONSE = "LOGIN_RESPONSE";


    public static final String REGISTER_REQUEST = "REGISTER_REQUEST";
    public static final String REGISTER_RESPONSE = "REGISTER_RESPONSE";

    // Yêu cầu lấy sản phẩm đang đấu giá (ON_AUCTION)
    public static final String GET_AUCTION_PRODUCT_REQUEST  = "GET_AUCTION_PRODUCT_REQUEST";
    public static final String GET_AUCTION_PRODUCT_RESPONSE = "GET_AUCTION_PRODUCT_RESPONSE";

    // Yêu cầu lấy danh sách sản phẩm của shop (AVAILABLE/SOLD...)
    public static final String GET_SHOP_PRODUCTS_REQUEST  = "GET_SHOP_PRODUCTS_REQUEST";
    public static final String GET_SHOP_PRODUCTS_RESPONSE = "GET_SHOP_PRODUCTS_RESPONSE";

    // (Sau này làm tính năng gì thì cứ khai báo thêm vào đây)
}