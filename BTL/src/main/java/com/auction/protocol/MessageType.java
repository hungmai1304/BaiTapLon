package com.auction.protocol;
// Enum liệt kê các loại tin nhắn trao đổi giữa Client và Server
public enum MessageType implements Serializable {
    // 1. Các lệnh về Đăng nhập/Đăng ký
    LOGIN_REQUEST,
    LOGIN_RESPONSE,
    REGISTER_REQUEST,
    REGISTER_RESPONSE,

    // 2. Các lệnh của quá trình đấu giá
    GET_AUCTION_LIST_REQUEST,    // Lấy danh sách các phiên đấu giá
    GET_AUCTION_LIST_RESPONSE,   // Trả về danh sách
    PLACE_BID_REQUEST,           // Yêu cầu đặt giá (Bid)
    PLACE_BID_RESPONSE,          // Trả lời đặt giá (Thành công/Thất bại)

    // 3. Các lệnh thời gian thực (Real-time)
    UPDATE_PRICE_BROADCAST,      // Server loan báo giá mới cho tất cả Client
    AUCTION_END_BROADCAST,       // Server thông báo phiên kết thúc, chốt người thắng

    // 4. Báo lỗi hệ thống
    ERROR_RESPONSE
}