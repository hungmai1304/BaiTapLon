package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.auction.server.dao.UserDao; // Khai báo vũ khí UserDao
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.sql.Timestamp;
import java.util.Map;
import java.util.UUID; // Thư viện tạo ID ngẫu nhiên

@CommandMap(value = MessageType.REGISTER_REQUEST)
public class RegisterHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[RegisterHandler] Đang xử lý yêu cầu Đăng ký...");

        try {
            String name = (String) data.get("name");
            String email = (String) data.get("email");
            String password = (String) data.get("password");
            String role = (String) data.get("role"); // Sẽ là "SELLER" hoặc "BUYER"

            // Sinh ID ngẫu nhiên và lấy thời gian đăng ký hiện tại
            String id = UUID.randomUUID().toString();
            Timestamp timeCreated = new Timestamp(System.currentTimeMillis());

            System.out.println("=> Khách mới: " + name + " | Email: " + email + " | Role: " + role);

            //  LOGIC ĐĂNG KÝ VỚI DATABASE
            boolean isSuccess = false;

            // Kiểm tra vai trò để phân luồng gọi đúng hàm
            if ("SELLER".equalsIgnoreCase(role)) {
                // Nếu là người bán, lấy thêm tên shop
                String shopName = (String) data.get("shopName");
                isSuccess = UserDao.getInstance().insertSeller(email, password, name, id, timeCreated, shopName);
            } else {
                // Nếu là người mua (BUYER), gọi hàm Bidder
                isSuccess = UserDao.getInstance().insertBidder(email, password, name, id, timeCreated);
            }

            // TRẢ KẾT QUẢ CHO CLIENT
            if (isSuccess) {
                // --- ĐĂNG KÝ THÀNH CÔNG ---
                Response response = new Response(
                        MessageType.REGISTER_RESPONSE,
                        "SUCCESS",
                        "Đăng ký thành công! Chào mừng " + name + " gia nhập hệ thống."
                );
                conn.send(gson.toJson(response));
                System.out.println("[RegisterHandler] Đã lưu vào DB và báo thành công cho Client.");
            } else {
                // --- ĐĂNG KÝ THẤT BẠI (Do trùng email hoặc rớt mạng) ---
                Response response = new Response(
                        MessageType.REGISTER_RESPONSE,
                        "ERROR",
                        "Đăng ký thất bại! Email này có thể đã tồn tại."
                );
                conn.send(gson.toJson(response));
                System.out.println("[RegisterHandler] Lỗi lưu vào DB (có thể do trùng email).");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Response response = new Response(
                    MessageType.REGISTER_RESPONSE,
                    "ERROR",
                    "Lỗi Server khi đăng ký: " + e.getMessage()
            );
            conn.send(gson.toJson(response));
        }
    }
}