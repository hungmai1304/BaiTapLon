package com.auction.common.model;


import com.auction.common.model.*;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== KIỂM TRA HỆ THỐNG PHÂN CẤP ĐỐI TƯỢNG ===");

        // 1. Khởi tạo Admin (Kế thừa trực tiếp từ User)
        Admin admin = new Admin("admin_enix", "password123");

        // 2. Khởi tạo Bidder (Kế thừa từ User, có thêm Email và Status)
        Bidder bidder = new Bidder("bidder_01", "bid123", "bidder@gmail.com");
        bidder.setStatus(Bidder.BidderStatus.BLACKLISTED); // Thử đổi trạng thái

        // 3. Sử dụng tính Đa hình (Polymorphism)
        // Mọi Admin và Bidder đều là User, nên ta có thể bỏ chung vào một danh sách
        List<User> userList = new ArrayList<>();
        userList.add(admin);
        userList.add(bidder);

        System.out.println("\n--- Danh sách người dùng trong hệ thống ---");
        for (User u : userList) {
            System.out.println("Kiểu đối tượng: " + u.getClass().getSimpleName());
            System.out.println("Dữ liệu: " + u.toString());
            System.out.println("------------------------------------------");
        }

        // 4. Kiểm tra logic xử lý dựa trên kiểu đối tượng
        System.out.println("\n--- Kiểm tra Logic quyền hạn ---");
        for (User u : userList) {
            if (u instanceof Admin) {
                System.out.println("User [" + u.getName() + "] có quyền QUẢN TRỊ VIÊN.");
            } else if (u instanceof Bidder) {
                Bidder b = (Bidder) u; // Ép kiểu về Bidder để xem email/status
                System.out.println("User [" + b.getName() + "] là NGƯỜI ĐẤU GIÁ.");
                System.out.println(" -> Email: " + b.getEmail());
                System.out.println(" -> Trạng thái: " + b.getStatus());
            }
        }
    }
}
