package com.auction.server.service;



// toàn bộ code trong file này chỉ là thử nghiệm và có thể xóa đi----------------------------------

// yêu cầu viết lại logic
//XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX





public class AuctionServer {

    public static boolean checkLogin(String email, String password) {
        // Giả sử logic kiểm tra đơn giản
        if (email.contains("@") && password.length() >= 6) {
            return true;
        }
        return false;
    }
}
