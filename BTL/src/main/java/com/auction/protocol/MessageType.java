package com.auction.protocol;

public class MessageType {
    // Các tín hiệu liên quan đến đăng nhập
    public static final String LOGIN_REQUEST = "LOGIN_REQUEST";
    public static final String LOGIN_RESPONSE = "LOGIN_RESPONSE";

    // Các tín hiệu liên quan đến đăng ký
    public static final String REGISTER_REQUEST = "REGISTER_REQUEST";
    public static final String REGISTER_RESPONSE = "REGISTER_RESPONSE";

    // dang xuat khoi the gioi
    public static final String LOGOUT_REQUEST = "LOGOUT_REQUEST";
//    public static final String REGISTER_RESPONSE = "REGISTER_RESPONSE";


    //admin login

    public static final String ADMIN_LOGIN_RESPONSE="ADMIN_LOGIN_RESPONSE";


    // Yêu cầu lấy danh sách sản phẩm của shop (AVAILABLE/SOLD...)
    public static final String GET_SHOP_PRODUCTS_REQUEST  = "GET_SHOP_PRODUCTS_REQUEST";
    public static final String GET_SHOP_PRODUCTS_RESPONSE = "GET_SHOP_PRODUCTS_RESPONSE";

    // Nhập sản phẩm vào kho của Shop
    public static final String IMPORT_PRODUCT_REQUEST = "IMPORT_PRODUCT_REQUEST";
    public static final String IMPORT_PRODUCT_RESPONSE = "IMPORT_PRODUCT_RESPONSE";

    // Shop bấm nút đăng bán món hàng mới lên sàn
    public static final String SELL_PRODUCT_REQUEST = "SELL_PRODUCT_REQUEST";
    public static final String SELL_PRODUCT_RESPONSE = "SELL_PRODUCT_RESPONSE";

    // Shop ấn lưu thay đổi thông tin món hàng (giá cả, tên, mô tả...)
    public static final String EDIT_PRODUCT_REQUEST = "EDIT_PRODUCT_REQUEST";
    public static final String EDIT_PRODUCT_RESPONSE = "EDIT_PRODUCT_RESPONSE";

    // Server thông báo với client (Dùng sau khi có người vừa đăng bán thành công)
    public static final String UPDATE_AUCTION_LIST_RESPONSE = "UPDATE_AUCTION_LIST_RESPONSE";

    // Hệ thống lắng nghe thời gian (TikTok Listener)
    public static final String TIK_TOK_LISTENER_REQUEST = "TIK_TOK_LISTENER_REQUEST";
    public static final String TIK_TOK_LISTENER_RESPONSE = "TIK_TOK_LISTENER_RESPONSE";

    // Ngắt hệ thống lắng nghe thời gian
    public static final String STOP_TIK_TOK_LISTENER_REQUEST = "STOP_TIK_TOK_LISTENER_REQUEST";
    public static final String STOP_TIK_TOK_LISTENER_RESPONSE = "STOP_TIK_TOK_LISTENER_RESPONSE";

    // 1. Client xin danh sách các phiên đấu giá đang diễn ra
    public static final String GET_ACTIVE_AUCTIONS_REQUEST = "GET_ACTIVE_AUCTIONS_REQUEST";
    public static final String GET_ACTIVE_AUCTIONS_RESPONSE = "GET_ACTIVE_AUCTIONS_RESPONSE";

    // 2. Server broadcast khi có 1 phiên đấu giá mới vừa được Shop đưa lên sàn
    public static final String UPDATE_ACTIVE_AUCTIONS_RESPONSE = "UPDATE_ACTIVE_AUCTIONS_RESPONSE";

    // 3. Client gửi lệnh xin ra giá (Bid)
    public static final String PLACE_BID_REQUEST = "PLACE_BID_REQUEST";
    public static final String PLACE_BID_RESPONSE = "PLACE_BID_RESPONSE";

    // 4. Server broadcast cho cả sàn biết vừa có giá mới
    public static final String BROADCAST_NEW_BID = "BROADCAST_NEW_BID";

    // =========================================================================
    // 🆕 CÁC TÍN HIỆU ĐƯỢC THÊM MỚI (VÍ TRÚC VÀ GIAO DỊCH TÀI KHOẢN)
    // =========================================================================

    // Tín hiệu xử lý nạp tiền (Deposit)
    public static final String DEPOSIT_REQUEST = "DEPOSIT_REQUEST";
    public static final String DEPOSIT_RESPONSE = "DEPOSIT_RESPONSE";

    // Tín hiệu xử lý rút tiền (Withdraw)
    public static final String WITHDRAW_REQUEST = "WITHDRAW_REQUEST";
    public static final String WITHDRAW_RESPONSE = "WITHDRAW_RESPONSE";

    // Tín hiệu đồng bộ số dư tài khoản (Ví dụ: Trả ví về khi vừa đăng nhập xong)
    public static final String GET_BALANCE_REQUEST = "GET_BALANCE_REQUEST";
    public static final String GET_BALANCE_RESPONSE = "GET_BALANCE_RESPONSE";

    // =========================================================================
    //  CÁC TÍN HIỆU DỰ PHÒNG CHO LOGIC KẾT THÚC ĐẤU GIÁ
    // =========================================================================

    // Server thông báo phiên đấu giá đã kết thúc thành công (Có người thắng) hoặc thất bại (Không ai mua)
    public static final String AUCTION_ENDED_BROADCAST = "AUCTION_ENDED_BROADCAST";

    // Người thắng cuộc vào nhận sản phẩm và hệ thống tự động trừ tiền trong ví
    public static final String CLAIM_PRIZE_REQUEST = "CLAIM_PRIZE_REQUEST";
    public static final String CLAIM_PRIZE_RESPONSE = "CLAIM_PRIZE_RESPONSE";

    // xoa san pham
    public static final String DELETE_PRODUCT_REQUEST = "DELETE_PRODUCT_REQUEST";
    public static final String DELETE_PRODUCT_RESPONSE = "DELETE_PRODUCT_RESPONSE";

    // deo biet tin nhan la gi
    public static final String OTHER="OTHER";


    // Admin quản lý danh sách người dùng online
    public static final String GET_ONLINE_USERS_REQUEST = "ADMIN_GET_ONLINE_USERS_REQUEST";
    public static final String GET_ONLINE_USERS_RESPONSE = "ADMIN_GET_ONLINE_USERS_RESPONSE";

    public static final String ADMIN_ONLINE_AUCTIONS_LIST_REQUEST="ADMIN_ONLINE_AUCTIONS_LIST";
    public static final String ADMIN_ONLINE_AUCTIONS_LIST_RESPONSE="ADMIN_ONLINE_AUCTIONS_LIST_RESPONSE";

    public static final String GET_ADMIN_REQUEST_LIST = "GET_ADMIN_REQUEST_LIST";
    public static final String GET_ADMIN_REQUEST_LIST_RESPONSE = "GET_ADMIN_REQUEST_LIST_RESPONSE";

    public static final String ADMIN_ACCEPT_REQUEST="ADMIN_ACCEPT_REQUEST";
    public static final String ADMIN_REJECT_REQUEST="ADMIN_REJECT_REQUEST";

    // Cập nhật Avatar
    public static final String UPDATE_AVATAR_REQUEST = "UPDATE_AVATAR_REQUEST";
    public static final String UPDATE_AVATAR_RESPONSE = "UPDATE_AVATAR_RESPONSE";

    // botBidding
    public static final String REGISTER_BOT_REQUEST = "REGISTER_BOT_REQUEST";
    public static final String REGISTER_BOT_RESPONSE = "REGISTER_BOT_RESPONSE";
}