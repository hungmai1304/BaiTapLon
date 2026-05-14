package com.auction.protocol;

public class MessageType {
    // Các tín hiệu liên quan đến Đăng nhập
    public static final String LOGIN_REQUEST = "LOGIN_REQUEST";
    public static final String LOGIN_RESPONSE = "LOGIN_RESPONSE";

    public static final String REGISTER_REQUEST = "REGISTER_REQUEST";
    public static final String REGISTER_RESPONSE = "REGISTER_RESPONSE";

    // ===============================================================
    // CÁC GIAO KÈO ĐÃ CHỐT VỚI SERVER CHO TÍNH NĂNG ĐẤU GIÁ
    // ===============================================================

    // --- CÁCH SỬA AN TOÀN: Giữ lại tên cũ để HomeController không bị lỗi đỏ ---
    public static final String GET_AUCTION_PRODUCT_REQUEST  = "GET_ACTIVE_AUCTIONS_REQUEST";
    public static final String GET_AUCTION_PRODUCT_RESPONSE = "GET_ACTIVE_AUCTIONS_RESPONSE";

    // --- Giữ đúng tên mới mà bạn Server yêu cầu ---
    public static final String GET_ACTIVE_AUCTIONS_REQUEST = "GET_ACTIVE_AUCTIONS_REQUEST";
    public static final String GET_ACTIVE_AUCTIONS_RESPONSE = "GET_ACTIVE_AUCTIONS_RESPONSE";

    // 2. Server broadcast khi có 1 phiên đấu giá mới được đưa lên sàn
    public static final String UPDATE_ACTIVE_AUCTIONS_RESPONSE = "UPDATE_ACTIVE_AUCTIONS_RESPONSE";

    // 3. Client gửi lệnh xin ra giá (Bid)
    public static final String PLACE_BID_REQUEST = "PLACE_BID_REQUEST";
    public static final String PLACE_BID_RESPONSE = "PLACE_BID_RESPONSE";

    // 4. Server broadcast cho cả sàn biết vừa có giá mới
    public static final String BROADCAST_NEW_BID = "BROADCAST_NEW_BID";

    // ===============================================================

    // Yêu cầu lấy danh sách sản phẩm của shop (AVAILABLE/SOLD...)
    public static final String GET_SHOP_PRODUCTS_REQUEST  = "GET_SHOP_PRODUCTS_REQUEST";
    public static final String GET_SHOP_PRODUCTS_RESPONSE = "GET_SHOP_PRODUCTS_RESPONSE";

    public static final String IMPORT_PRODUCT_REQUEST ="IMPORT_PRODUCT_REQUEST";
    public static final String IMPORT_PRODUCT_RESPONSE ="IMPORT_PRODUCT_RESPONSE";

    // Shop bấm nút đăng bán món hàng mới lên sàn
    public static final String SELL_PRODUCT_REQUEST = "SELL_PRODUCT_REQUEST";
    public static final String SELL_PRODUCT_RESPONSE = "SELL_PRODUCT_RESPONSE";

    // Shop ấn lưu thay đổi thông tin món hàng (giá cả, tên, mô tả...)
    public static final String EDIT_PRODUCT_REQUEST = "EDIT_PRODUCT_REQUEST";
    public static final String EDIT_PRODUCT_RESPONSE = "EDIT_PRODUCT_RESPONSE";

    // Thêm dòng này để cứu file SellProductHandler và các file cũ
    public static final String UPDATE_AUCTION_LIST_RESPONSE = "UPDATE_ACTIVE_AUCTIONS_RESPONSE";

    // (Sau này làm tính năng gì thì cứ khai báo thêm vào đây)

}