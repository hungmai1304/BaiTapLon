package com.auction.server.handler.admin;

import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.server.service.ResponseSender;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandMap("ADMIN_LET_USER_UNBAN")
public class AdminLetUserUnban implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        String responseType = "ADMIN_GET_BANNED_LIST_RESPONSE"; // Trả thẳng về kiểu dữ liệu danh sách để Client tự cập nhật UI
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", responseType);

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminLetUserUnban] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            ResponseSender.send(conn, "ADMIN_UNBAN_RESPONSE", "ERROR", "Bạn cần đăng nhập để thực hiện thao tác này!", null);
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminLetUserUnban] Cảnh báo: Tài khoản " + adminEmail + " cố tình hack quyền UNBAN!");
            ResponseSender.send(conn, "ADMIN_UNBAN_RESPONSE", "ERROR", "Bạn không có quyền thực hiện hành động này!", null);
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ MỞ KHÓA TÀI KHOẢN (Set Status về NORMAL)
        // =========================================================================
        String targetEmail = (String) data.get("email");

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            System.err.println("[AdminLetUserUnban] Thất bại: Thiếu thông tin email của user cần mở khóa!");
            ResponseSender.send(conn, "ADMIN_UNBAN_RESPONSE", "ERROR", "Email người dùng không hợp lệ!", null);
            return;
        }

        System.out.println("[AdminLetUserUnban] Admin [" + adminEmail + "] yêu cầu UNBAN tài khoản user: " + targetEmail);

        // Cập nhật trạng thái xuống Database thành NORMAL
        boolean isDbUpdated = userDao.updateUserStatus(targetEmail, "NORMAL");
        if (!isDbUpdated) {
            System.err.println("[AdminLetUserUnban] Thất bại: Không thể cập nhật trạng thái NORMAL trong DB cho: " + targetEmail);
            ResponseSender.send(conn, "ADMIN_UNBAN_RESPONSE", "ERROR", "Không thể mở khóa tài khoản trong cơ sở dữ liệu!", null);
            return;
        }

        // Cập nhật lại cả trạng thái trên RAM nếu user này đang được lưu giữ trong Context
        context.updateUserStatusInRam(targetEmail, "NORMAL");

        // =========================================================================
        // 3. ĐỒNG BỘ: LẤY LẠI DANH SÁCH BANNED MỚI NHẤT VÀ GỬI VỀ CHO ADMIN
        // =========================================================================
        System.out.println("[AdminLetUserUnban] Đang lấy lại danh sách tài khoản bị khóa mới nhất để đồng bộ Client...");

        try {
            // Lấy danh sách còn lại từ DB sau khi đã tha bổng tài khoản trên
            List<User> bannedUsers = userDao.getUsersByStatus("BANNED");

            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Mở khóa tài khoản thành công!");

            // Đóng gói data theo cấu trúc Client mong đợi: { data: { list: [...] } }
            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("list", bannedUsers);
            responseMap.put("data", dataMap);

            // Tiến hành gửi chuỗi JSON quay lại cho Admin
            conn.send(gson.toJson(responseMap));
            System.out.println("[AdminLetUserUnban] Đã làm mới và gửi lại " + bannedUsers.size() + " tài khoản bị khóa cho Admin.");

        } catch (Exception e) {
            System.err.println("[AdminLetUserUnban] Lỗi hệ thống khi làm mới danh sách: " + e.getMessage());

            responseMap.put("status", "ERROR");
            responseMap.put("message", "Mở khóa thành công nhưng có lỗi khi tải lại danh sách mới!");
            responseMap.put("data", new HashMap<>());
            conn.send(gson.toJson(responseMap));
        }
    }
}