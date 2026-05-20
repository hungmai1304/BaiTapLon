package com.auction.server.handler;

import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.auction.common.model.user.User; // Giả định đúng package của User model
import com.auction.server.dao.UserDao;       // Giả định đúng package của UserDao
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.Map;

@CommandMap("BACK_TO_ADMIN_COMMAND")
public class BackToAdminHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "BACK_TO_ADMIN_RESPONSE");

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================

        // Bước 1.1: Kiểm tra xem kết nối (conn) này đã đăng nhập vào hệ thống chưa
        String loggedInEmail = context.getUserByConn(conn);
        if (loggedInEmail == null) {
            System.err.println("[BackToAdminHandler] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // Lấy email truyền lên từ Client data để đối chiếu chéo (Cross-check phòng hờ giả mạo)
        String clientProvidedEmail = (String) data.get("data"); // Ở HomeController bạn gửi chuỗi Email làm data payload

        // Bước 1.2: Lấy thông tin user từ DB để chắc chắn người này có quyền ADMIN
        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(loggedInEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[BackToAdminHandler] Cảnh báo: Tài khoản " + loggedInEmail + " cố tình hack quyền Admin!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn đã bị cấm mãi mãi hoặc không có quyền!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. PHẢN HỒI THÀNH CÔNG
        // =========================================================================
        System.out.println("[BackToAdminHandler] Xác thực thành công! Cho phép Admin " + loggedInEmail + " quay lại bảng quản trị.");
        responseMap.put("status", "SUCCESS");
        responseMap.put("message", "Xác thực Admin thành công!");

        conn.send(gson.toJson(responseMap));
    }
}