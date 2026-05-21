package com.auction.common.model.product;

public enum ProductStatus {
    AVAILABLE,   // Sản phẩm đang chờ, chưa được đưa lên đấu giá
    ON_AUCTION,  // Đang được đấu giá trên sàn
    SOLD,        // Đã bán xong
    CANCELLED ,   // Bị hủy (seller rút, hoặc không ai đấu)
    NOT_AVAILABLE //bi tao kick
}