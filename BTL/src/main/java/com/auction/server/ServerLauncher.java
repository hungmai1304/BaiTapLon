package com.auction.server;

import com.auction.common.model.auction.Auction;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import java.time.LocalDateTime;

public class ServerLauncher {

    public static void main(String[] args) {
        // 1. Ép Server luôn chạy ở múi giờ Việt Nam (GMT+7) để tránh lệch giờ khi deploy lên Render/Cloud
        java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));

        try {
            // 2. Lấy PORT linh hoạt từ biến môi trường của Render, nếu không có mới dùng 10000
            String portStr = System.getenv("PORT");
            int port;
            if (portStr != null && !portStr.isEmpty()) {
                port = Integer.parseInt(portStr);
            } else {
                port = 10000;
            }

            System.out.println("=========================================");
            System.out.println("🚀 SERVER ĐẤU GIÁ ĐANG KHỞI ĐỘNG (RENDER)");
            System.out.println("PORT: " + port);
            System.out.println("TIMEZONE: " + java.util.TimeZone.getDefault().getID());
            System.out.println("=========================================");

            // 3. Khởi chạy WebSocket Server
            AuctionWebSocketServer server = new AuctionWebSocketServer(port);
            server.start();

            System.out.println("✅ Server đã start thành công.");
            System.out.println("📡 Đang chờ các Client kết nối... ");

            // =========================================================================
            // CHỈ CHẠY ĐÚNG 1 LẦN DUY NHẤT KHI MỞ SERVER ĐỂ DỌN RÁC DỮ LIỆU CŨ
            // =========================================================================
            try {
                System.out.println("🤖 [SystemCheck] Đang quét kiểm tra sản phẩm hết hạn lúc mở Server...");
                ServerContext context = ServerContext.getInstance();

                // 4. Quét sạch các phiên đấu giá lỗi thời trên RAM (ConcurrentModificationException-safe)
                java.util.List<String> expiredProductIds = new java.util.ArrayList<>();
                for (Auction a : context.getActiveAuctions()) {
                    if (a.getProduct() != null && a.getProduct().getEndTime() != null) {
                        if (a.getProduct().getEndTime().isBefore(LocalDateTime.now())) {
                            expiredProductIds.add(a.getProduct().getId());
                        }
                    }
                }
                for (String id : expiredProductIds) {
                    context.removeAuctionByProductId(id);
                }

                // 5. Cập nhật dứt điểm trạng thái dưới Database cho các sản phẩm đã hết giờ trong lúc Server tắt
                int affectedRows = ProductDao.getInstance().autoExpireProducts();
                System.out.println("🤖 [SystemCheck] Hoàn thành quét dọn! Đã hạ sàn " + affectedRows + " sản phẩm hết hạn dưới DB.");

            } catch (Exception e) {
                System.err.println("❌ Lỗi khi quét dọn lúc mở Server: " + e.getMessage());
            }
            // =========================================================================

            // 6. GIỮ SERVER SỐNG TRÊN RENDER (Tránh Thread main kết thúc làm sập app)
            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}