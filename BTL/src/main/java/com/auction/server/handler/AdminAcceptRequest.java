package com.auction.server.handler;

import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.Map;

@CommandMap(MessageType.ADMIN_ACCEPT_REQUEST)
public class AdminAcceptRequest implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {

        // Tạo một Map để chuẩn bị phản hồi kết quả về cho Client
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", MessageType.ADMIN_ACCEPT_REQUEST.toString());

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================

        // Bước 1.1: Kiểm tra xem kết nối (conn) này đã đăng nhập vào hệ thống chưa
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminAcceptRequest] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // Bước 1.2: Lấy thông tin user từ DB để chắc chắn người này có quyền ADMIN
        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminAcceptRequest] Cảnh báo: Tài khoản " + adminEmail + " cố tình hack quyền duyệt Admin!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ LỆNH DUYỆT (Khi đã xác minh đúng chính chủ Admin)
        // =========================================================================

        // Lấy id của user cần duyệt từ trong gói tin dữ liệu gửi lên
        String userId = (String) data.get("userId");

        if (userId == null || userId.trim().isEmpty()) {
            System.err.println("[AdminAcceptRequest] Lỗi: Không tìm thấy hoặc ID cần duyệt bị trống!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "ID người dùng cần duyệt không hợp lệ.");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // Gọi UserDao cập nhật cột 'role' thành 'ADMIN' trong database
        boolean isUpdated = userDao.updateUserRole(userId, "ADMIN");

        // 3. Kiểm tra kết quả cập nhật và phản hồi lại qua WebSocket
        if (isUpdated) {
            System.out.println("[AdminAcceptRequest] Thành công: Admin [" + adminEmail + "] đã duyệt User [" + userId + "] thành ADMIN.");

            responseMap.put("status", "SUCCESS");
            responseMap.put("userId", userId);
            responseMap.put("message", "Đã duyệt quyền Admin thành công!");
        } else {
            System.err.println("[AdminAcceptRequest] Thất bại: Không thể cập nhật database cho User [" + userId + "].");

            responseMap.put("status", "FAILED");
            responseMap.put("message", "Cập nhật cơ sở dữ liệu thất bại. Vui lòng kiểm tra lại ID.");
        }

        // Gửi kết quả JSON về lại cho phía Client đã gửi request này
        conn.send(gson.toJson(responseMap));
    }
}