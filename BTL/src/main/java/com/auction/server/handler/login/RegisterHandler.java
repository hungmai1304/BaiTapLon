package com.auction.server.handler.login;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.auction.common.utils.Generate_id_and_timecreated;
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

            // --- ĐOẠN XỬ LÝ CHECK ADMIN MỚI THÊM VÀO ---
            if (isAdminEmail(email)) {
                role = "ADMIN_REQUEST";
            }
            // -------------------------------------------

            // 2. Tạo ID và TimeCreated
            LocalDateTime now = Generate_id_and_timecreated.getCurrentTimestamp2();
            String id = Generate_id_and_timecreated.hashTimestampToId(now.toString());
            Timestamp sqlTimestamp = Timestamp.valueOf(now); // Chuyển sang SQL Timestamp để lưu DB

            // Số dư mặc định ban đầu khi đăng ký tài khoản mới là 0.0
            double initialBalance = 0.0;

            // VIẾT THÊM: Khởi tạo trạng thái mặc định của tài khoản là "NORMAL"
            String initialStatus = "NORMAL";

            boolean isSuccess = false;

            // 3. LOGIC PHÂN LOẠI ĐĂNG KÝ (Đồng bộ thêm tham số initialStatus vào DAO)
            if ("SELLER".equalsIgnoreCase(role)) {
                // Nếu là Seller, lấy thêm shopName
                String shopName = (String) data.get("shopName");
                if (shopName == null || shopName.isEmpty()) {
                    sendError(conn, gson, "Người bán phải có tên Shop!");
                    return;
                }
                // Thêm tham số initialStatus vào cuối hàm insertSeller
                isSuccess = UserDao.getInstance().insertSeller(email, password, name, id, sqlTimestamp, shopName, initialBalance, initialStatus);
            } else {
                // Nhánh này xử lý chung cho cả "BIDDER" và "ADMIN_REQUEST" như tham số role động vừa thêm
                // Thêm tham số initialStatus vào cuối hàm insertBidder
                isSuccess = UserDao.getInstance().insertBidder(email, password, name, id, sqlTimestamp, initialBalance, role, initialStatus);
            }

            // 4. Trở về kết quả
            if (isSuccess) {
                Response response = new Response(
                        MessageType.REGISTER_RESPONSE,
                        "SUCCESS",
                        "Đăng ký thành công! Chào mừng " + name
                );
                conn.send(gson.toJson(response));
                System.out.println("[RegisterHandler] Đăng ký thành công cho: " + email + " với role: " + role + " | Status: " + initialStatus);
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

    /**
     * Hàm helper check email admin phục vụ riêng cho Handler này
     */
    private boolean isAdminEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        return email.trim().toLowerCase().endsWith("@admin.com");
    }
}