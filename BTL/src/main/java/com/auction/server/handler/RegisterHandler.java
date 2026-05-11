package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.auction.common.utils.Generate_id_and_timecreated; // Nhớ import cái này
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

@CommandMap(value = MessageType.REGISTER_REQUEST)
public class RegisterHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[RegisterHandler] Đang xử lý đăng ký...");

        try {
            // 1. Lấy dữ liệu cơ bản
            String name = (String) data.get("name");
            String email = (String) data.get("email");
            String password = (String) data.get("password");
            String role = (String) data.get("role");

            // 2. Tạo ID và TimeCreated (Dùng Utils của bạn)
            LocalDateTime now = Generate_id_and_timecreated.getCurrentTimestamp2();
            String id = Generate_id_and_timecreated.hashTimestampToId(now.toString());
            Timestamp sqlTimestamp = Timestamp.valueOf(now); // Chuyển sang SQL Timestamp để lưu DB

            boolean isSuccess = false;

            // 3. LOGIC PHÂN LOẠI ĐĂNG KÝ
            if ("SELLER".equalsIgnoreCase(role)) {
                // Nếu là Seller, lấy thêm shopName
                String shopName = (String) data.get("shopName");
                if (shopName == null || shopName.isEmpty()) {
                    sendError(conn, gson, "Người bán phải có tên Shop!");
                    return;
                }
                isSuccess = UserDao.getInstance().insertSeller(email, password, name, id, sqlTimestamp, shopName);
            } else {
                // Nếu là Bidder
                isSuccess = UserDao.getInstance().insertBidder(email, password, name, id, sqlTimestamp);
            }

            // 4. Trả về kết quả
            if (isSuccess) {
                Response response = new Response(
                        MessageType.REGISTER_RESPONSE,
                        "SUCCESS",
                        "Đăng ký thành công! Chào mừng " + name
                );
                conn.send(gson.toJson(response));
                System.out.println("[RegisterHandler] Đăng ký thành công cho: " + email);
            } else {
                sendError(conn, gson, "Đăng ký thất bại! Email có thể đã tồn tại.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi Server: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.REGISTER_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}