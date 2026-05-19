package com.auction.server.handler;

import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.Map;

@CommandMap("ADMIN_LET_USER_BE_BANNED")
public class AdminLetUserBeBanned implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        Map<String, Object> responseMap = new HashMap<>();

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminLetUserBeBanned] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminLetUserBeBanned] Cảnh báo: Tài khoản " + adminEmail + " cố tình hack quyền BAN của Admin!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ BAN TÀI KHOẢN VÀ ĐẨY USER RA KHỎI HỆ THỐNG
        // =========================================================================

        // Bước 2.1: Lấy email của user cần ban từ Map data gửi lên
        String targetEmail = (String) data.get("email");

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            System.err.println("[AdminLetUserBeBanned] Thất bại: Thiếu thông tin email của user cần khóa!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Email người dùng không hợp lệ!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        System.out.println("[AdminLetUserBeBanned] Admin [" + adminEmail + "] yêu cầu BAN tài khoản user: " + targetEmail);

        // Bước 2.2: Gọi hàm cập nhật trạng thái "BANNED" xuống Database
        boolean isDbUpdated = userDao.updateUserStatus(targetEmail, "BANNED");
        if (!isDbUpdated) {
            System.err.println("[AdminLetUserBeBanned] Thất bại: Không thể cập nhật trạng thái BANNED trong DB cho: " + targetEmail);
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Không thể cập nhật trạng thái tài khoản trong cơ sở dữ liệu!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // Bước 2.3: Tìm kết nối WebSocket (conn) của user bị ban trên RAM trước khi xóa hẳn
        WebSocket targetConn = null;
        for (Map.Entry<WebSocket, User> entry : context.getOnlineUserObjects().entrySet()) {
            if (entry.getValue() != null && targetEmail.equalsIgnoreCase(entry.getValue().getEmail())) {
                targetConn = entry.getKey();
                break;
            }
        }

        // Bước 2.4: Gửi lệnh ép đăng xuất và thông báo lý do bị BAN về phía Client của User đó (nếu họ đang online)
        if (targetConn != null && targetConn.isOpen()) {
            // Cập nhật trạng thái đổi lập tức trên RAM để nếu Admin Broadcast danh sách sẽ nhận được text "BANNED" trước khi ngắt hẳn
            context.updateUserStatusInRam(targetEmail, "BANNED");

            Map<String, Object> forceLogoutSignal = new HashMap<>();
            forceLogoutSignal.put("type", "ADMIN_FORCE_LOGOUT");
            forceLogoutSignal.put("status", "SUCCESS");
            forceLogoutSignal.put("message", "Tài khoản của bạn đã bị khóa (BANNED) bởi Admin!");

            targetConn.send(gson.toJson(forceLogoutSignal));
            System.out.println("[AdminLetUserBeBanned] Đã gửi tín hiệu khóa tài khoản tới kết nối của: " + targetEmail);
        }

        // Bước 2.5: Tiến hành dọn dẹp kết nối ra khỏi RAM trên cả 2 danh sách quản lý
        if (targetConn != null) {
            context.removeUser(targetConn);
            System.out.println("[AdminLetUserBeBanned] Đã gọi context.removeUser(targetConn)");
        }

        context.removeOnlineUserByEmail(targetEmail);
        System.out.println("[AdminLetUserBeBanned] Đã gọi context.removeOnlineUserByEmail(targetEmail)");

        // =========================================================================
        // 3. PHẢN HỒI THÔNG BÁO VỀ CHO ADMIN PHÁT LỆNH
        // =========================================================================
        responseMap.put("status", "OTHER"); // Trả về dạng trạng thái OTHER theo yêu cầu của bạn
        responseMap.put("message", "Đã BAN người dùng có email: " + targetEmail);
        conn.send(gson.toJson(responseMap));
    }
}