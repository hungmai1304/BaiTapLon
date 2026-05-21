package com.auction.server.handler.admin;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.user.User;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandMap("ADMIN_GET_ONLINE_AUCTIONS")
public class AdminGetOnlineAuctionsRequest implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        // 1. Khởi tạo Map gói tin phản hồi công khai đúng định dạng bài trước
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "ADMIN_GET_ONLINE_AUCTIONS_RESPONSE");

        // =========================================================================
        // KIỂM TRA QUYỀN TRUY CẬP (Hạn chế tối đa nguy cơ Hack/Leak dữ liệu)
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminGetOnlineAuctions] Từ chối: Kết nối này chưa đăng nhập hệ thống!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminGetOnlineAuctions] Cảnh báo nguy hiểm: Tài khoản [" + adminEmail + "] cố gắng đọc danh sách Đấu giá Admin!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền hạn tối cao để thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. TRÍCH XUẤT VÀ TÁCH THÔNG TIN ĐẤU GIÁ TRÊN RAM (SERVER CONTEXT)
        // =========================================================================
        System.out.println("[AdminGetOnlineAuctions] Admin [" + adminEmail + "] đang quét danh sách phòng đấu giá trực tuyến...");

        try {
            // Lấy danh sách gốc an toàn từ bộ nhớ đệm RAM (Bọc ArrayList tránh ConcurrentModificationException)
            List<Auction> rawActiveAuctions;
            synchronized (context.getActiveAuctions()) {
                rawActiveAuctions = new ArrayList<>(context.getActiveAuctions());
            }

            // Tiến hành bóc tách (Flatten) dữ liệu đối tượng phức tạp thành cấu trúc JSON phẳng gọn nhẹ gửi về Client
            List<Map<String, Object>> formattedAuctions = new ArrayList<>();

            for (Auction auction : rawActiveAuctions) {
                if (auction == null) continue;

                Map<String, Object> flatItem = new HashMap<>();

                // Trích xuất các thuộc tính cốt lõi theo yêu cầu giao diện Client
                flatItem.put("id", auction.getId());

                // Trích xuất thông tin sản phẩm (Xử lý null bảo vệ hệ thống không bị crash đột ngột)
                if (auction.getProduct() != null) {
                    flatItem.put("productName", auction.getProduct().getName());
                    flatItem.put("status", auction.getProduct().getStatus()); // ADVERTISING hoặc ONGOING
                } else {
                    flatItem.put("productName", "Sản phẩm không xác định");
                    flatItem.put("status", "UNKNOWN");
                }

                // Trích xuất tên người đăng (Chủ sở hữu phiên đấu giá phòng)
                // Phụ thuộc vào thiết kế Model của bạn: lấy từ auction.getOwner() hoặc lấy từ product
                if (auction.getProduct() != null && auction.getProduct().getOwner().getId() != null) {
                    flatItem.put("ownerEmail", auction.getProduct().getOwner().getId());
                } else {
                    flatItem.put("ownerEmail", "Ẩn danh (N/A)");
                }

                formattedAuctions.add(flatItem);
            }

            // Gói cấu trúc dữ liệu theo đúng chuẩn xử lý mảng của Client
            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Tải danh sách phiên đấu giá trực tuyến thành công!");

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("list", formattedAuctions); // Đồng bộ trường "list" đổ thẳng vào TableView
            responseMap.put("data", dataMap);

            // Tiến hành mã hóa JSON và truyền tải qua luồng mạng Socket
            conn.send(gson.toJson(responseMap));
            System.out.println("[AdminGetOnlineAuctions] Đã đẩy thành công dữ liệu " + formattedAuctions.size() + " phiên đấu giá cho Admin.");

        } catch (Exception e) {
            System.err.println("[AdminGetOnlineAuctions] Lỗi hệ thống phát sinh: " + e.getMessage());
            e.printStackTrace();

            responseMap.put("status", "ERROR");
            responseMap.put("message", "Có lỗi xảy ra khi trích xuất dữ liệu RAM từ Server!");
            responseMap.put("data", new HashMap<>());
            conn.send(gson.toJson(responseMap));
        }
    }
}
// me: day la file get online Auctions nhe, de cap nhat tuc thoi cho admin thi: