package com.auction.protocol;

public class MessageType {
    // Các tín hiệu liên quan đến đăng nhập
    public static final String LOGIN_REQUEST = "LOGIN_REQUEST";

    // Các tín hiệu liên quan đến đăng ký
    public static final String REGISTER_REQUEST = "REGISTER_REQUEST";

    // dang xuat khoi the gioi
    public static final String LOGOUT_REQUEST = "LOGOUT_REQUEST";


    //admin login

    public static final String ADMIN_LOGIN_RESPONSE="ADMIN_LOGIN_RESPONSE";


    // Yêu cầu lấy danh sách sản phẩm của shop (AVAILABLE/SOLD...)
    public static final String GET_SHOP_PRODUCTS_REQUEST  = "GET_SHOP_PRODUCTS_REQUEST";


    // Nhập sản phẩm vào kho của Shop
    public static final String IMPORT_PRODUCT_REQUEST = "IMPORT_PRODUCT_REQUEST";


    // Shop bấm nút đăng bán món hàng mới lên sàn
    public static final String SELL_PRODUCT_REQUEST = "SELL_PRODUCT_REQUEST";


    // Shop ấn lưu thay đổi thông tin món hàng (giá cả, tên, mô tả...)
    public static final String EDIT_PRODUCT_REQUEST = "EDIT_PRODUCT_REQUEST";
    public static final String EDIT_PRODUCT_RESPONSE = "EDIT_PRODUCT_RESPONSE";

    // Server thông báo với client (Dùng sau khi có người vừa đăng bán thành công)


    // Hệ thống lắng nghe thời gian (TikTok Listener)
    public static final String TIK_TOK_LISTENER_REQUEST = "TIK_TOK_LISTENER_REQUEST";

    // Ngắt hệ thống lắng nghe thời gian
    public static final String STOP_TIK_TOK_LISTENER_REQUEST = "STOP_TIK_TOK_LISTENER_REQUEST";

    // 1. Client xin danh sách các phiên đấu giá đang diễn ra
    public static final String GET_ACTIVE_AUCTIONS_REQUEST = "GET_ACTIVE_AUCTIONS_REQUEST";

    // 2. Server broadcast khi có 1 phiên đấu giá mới vừa được Shop đưa lên sàn

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

    // Tín hiệu xử lý rút tiền (Withdraw)
    public static final String WITHDRAW_REQUEST = "WITHDRAW_REQUEST";

    // Tín hiệu đồng bộ số dư tài khoản (Ví dụ: Trả ví về khi vừa đăng nhập xong)
    public static final String GET_BALANCE_REQUEST = "GET_BALANCE_REQUEST";

    // =========================================================================
    //  CÁC TÍN HIỆU DỰ PHÒNG CHO LOGIC KẾT THÚC ĐẤU GIÁ
    // =========================================================================

    // xoa san pham
    public static final String DELETE_PRODUCT_REQUEST = "DELETE_PRODUCT_REQUEST";

    public static final String DELETE_PRODUCT_RESPONSE = "DELETE_PRODUCT_RESPONSE";
    // tin nhan khong biet
    public static final String OTHER="OTHER";


    // Admin quản lý danh sách người dùng online
    public static final String GET_ONLINE_USERS_REQUEST = "ADMIN_GET_ONLINE_USERS_REQUEST";
    public static final String GET_ONLINE_USERS_RESPONSE = "ADMIN_GET_ONLINE_USERS_RESPONSE";


    public static final String GET_ADMIN_REQUEST_LIST = "GET_ADMIN_REQUEST_LIST";
    public static final String GET_ADMIN_REQUEST_LIST_RESPONSE = "GET_ADMIN_REQUEST_LIST_RESPONSE";

    public static final String ADMIN_ACCEPT_REQUEST="ADMIN_ACCEPT_REQUEST";
    public static final String ADMIN_REJECT_REQUEST="ADMIN_REJECT_REQUEST";

    // Yêu cầu lấy danh sách phiên đấu giá đã thắng của user
    public static final String GET_WON_AUCTIONS_REQUEST = "GET_WON_AUCTIONS_REQUEST";
    public static final String GET_WON_AUCTIONS_RESPONSE = "GET_WON_AUCTIONS_RESPONSE";

    // Cập nhật Avatar
    public static final String UPDATE_AVATAR_REQUEST = "UPDATE_AVATAR_REQUEST";
    public static final String UPDATE_AVATAR_RESPONSE = "UPDATE_AVATAR_RESPONSE";

    // botBidding
    public static final String REGISTER_BOT_REQUEST = "REGISTER_BOT_REQUEST";
}