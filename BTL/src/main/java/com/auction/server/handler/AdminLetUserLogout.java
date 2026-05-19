package com.auction.server.handler;

import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.Map;

@CommandMap("ADMIN_LET_USER_LOGOUT")
public class AdminLetUserLogout implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        Map<String, Object> responseMap = new HashMap<>();

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminLetUserLogout] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminLetUserLogout] Cảnh báo: Tài khoản " + adminEmail + " cố tình hack quyền đăng xuất Admin!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ ĐĂNG XUẤT VÀ XÓA USER TRÊN CẢ 2 HÀM CỦA SERVERCONTEXT
        // =========================================================================

        // Bước 2.1: Lấy email của user cần kick từ Map data gửi lên
        String targetEmail = (String) data.get("email");

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            System.err.println("[AdminLetUserLogout] Thất bại: Thiếu thông tin email của user cần kick!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Email người dùng không hợp lệ!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        System.out.println("[AdminLetUserLogout] Admin [" + adminEmail + "] yêu cầu ép đăng xuất user: " + targetEmail);

        // Bước 2.2: Tìm kết nối WebSocket (conn) của user bị chỉ định trước khi thực hiện xóa
        WebSocket targetConn = null;
        for (Map.Entry<WebSocket, User> entry : context.getOnlineUserObjects().entrySet()) {
            if (entry.getValue() != null && targetEmail.equalsIgnoreCase(entry.getValue().getEmail())) {
                targetConn = entry.getKey();
                break;
            }
        }

        // Bước 2.3: Gửi lệnh ép đăng xuất về phía Client của User bị chỉ định
        // =========================================================================

        if (targetConn != null && targetConn.isOpen()) {
            // Tạo cấu trúc Object JSON để Client không bị lỗi ép kiểu dữ liệu
            Map<String, Object> forceLogoutSignal = new HashMap<>();
            forceLogoutSignal.put("type", "ADMIN_FORCE_LOGOUT");
            forceLogoutSignal.put("status", "SUCCESS");

            // Ép thành chuỗi JSON: {"type":"ADMIN_FORCE_LOGOUT","status":"SUCCESS"}
            targetConn.send(gson.toJson(forceLogoutSignal));

            System.out.println("[AdminLetUserLogout] Đã gửi JSON ADMIN_FORCE_LOGOUT tới kết nối của: " + targetEmail);
        }

        // Bước 2.4: THỰC HIỆN XÓA TRÊN CẢ 2 HÀM NHƯ BẠN YÊU CẦU

        // 1. Gọi hàm xóa bằng Kết nối (Xóa trong onlineUsers và dọn dẹp tiktok listener)
        if (targetConn != null) {
            context.removeUser(targetConn);
            System.out.println("[AdminLetUserLogout] Đã gọi context.removeUser(targetConn)");
        }

        // 2. Gọi hàm xóa bằng Email (Xóa triệt để trong onlineUsers, onlineUserObjects, dọn dẹp listener và Broadcast)
        context.removeOnlineUserByEmail(targetEmail);
        System.out.println("[AdminLetUserLogout] Đã gọi context.removeOnlineUserByEmail(targetEmail)");


        // Bước 2.5: Phản hồi thông báo thành công về cho Admin phát lệnh
        responseMap.put("status", "SUCCESS");
        responseMap.put("message", "Đã buộc đăng xuất người dùng " + targetEmail + " thành công!");
        conn.send(gson.toJson(responseMap));
    }

}