package com.auction.server.handler.admin;

import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
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

@CommandMap("ADMIN_GET_ALL_SHOP")
public class AdminGetAllShop implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        // 1. Tạo Map tổng cho gói tin phản hồi
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "ADMIN_GET_ALL_SHOP_RESPONSE");

        // =========================================================================
        // KIỂM TRA CHÍNH CHỦ ADMIN
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminGetAllShop] Từ chối: Kết nối chưa đăng nhập!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminGetAllShop] Cảnh báo: " + adminEmail + " hack quyền xem danh sách Shop!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ LẤY DANH SÁCH SHOP (SELLER) VÀ ĐẾM SỐ SẢN PHẨM
        // =========================================================================
        System.out.println("[AdminGetAllShop] Admin [" + adminEmail + "] đang yêu cầu lấy danh sách toàn bộ Shop.");

        try {
            // Bước 2.1: Gọi hàm lấy toàn bộ tài khoản có Role là SELLER
            List<User> sellers = userDao.getUsersByRole("SELLER");

            // Bước 2.2: Tạo một danh sách mới chứa các Map đối tượng đã custom thông tin theo yêu cầu
            List<Map<String, Object>> shopListWithProductCount = new ArrayList<>();

            for (User seller : sellers) {
                Map<String, Object> shopInfo = new HashMap<>();

                // Nhặt các thông tin cơ bản của Shop
                shopInfo.put("id", seller.getId());
                shopInfo.put("email", seller.getEmail());
                shopInfo.put("name", seller.getUsername());
                shopInfo.put("shopName", seller.getShopName());
                shopInfo.put("status", seller.getStatus()); // Gửi thêm status để biết shop có đang bị BAN/BLACKLIST không

                // Gọi hàm đếm số lượng sản phẩm từ Database thông qua ID
                int productCount = userDao.countProductsByOwnerId(seller.getId());
                shopInfo.put("productCount", productCount);

                // Cho vào danh sách kết quả
                shopListWithProductCount.add(shopInfo);
            }

            // Bước 2.3: Đóng gói dữ liệu trả về theo đúng cấu trúc JSON Object { data: { list: [...] } } giống bên BannedList
            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Lấy danh sách toàn bộ cửa hàng thành công!");

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("list", shopListWithProductCount);
            responseMap.put("data", dataMap);

            // Gửi dữ liệu về Client
            conn.send(gson.toJson(responseMap));
            System.out.println("[AdminGetAllShop] Đã gửi thành công thông tin của " + shopListWithProductCount.size() + " Shop cho Admin.");

        } catch (Exception e) {
            System.err.println("[AdminGetAllShop] Lỗi hệ thống: " + e.getMessage());
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Có lỗi xảy ra khi truy vấn dữ liệu các Shop từ Server!");
            responseMap.put("data", new HashMap<>());
            conn.send(gson.toJson(responseMap));
        }
    }
}