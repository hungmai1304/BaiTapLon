package com.auction.server.handler.admin;

import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.server.service.ResponseSender;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

@CommandMap("ADMIN_LET_USER_BE_BANNED")
public class AdminLetUserBeBanned implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        String responseType = "ADMIN_LET_USER_BE_BANNED_RESPONSE";

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminLetUserBeBanned] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            ResponseSender.send(conn, responseType, "ERROR", "Bạn cần đăng nhập để thực hiện thao tác này!", null);
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminLetUserBeBanned] Cảnh báo: Tài khoản " + adminEmail + " cố tình hack quyền BAN!");
            ResponseSender.send(conn, responseType, "ERROR", "Bạn không có quyền thực hiện hành động này!", null);
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ BAN TÀI KHOẢN VÀ ĐẨY USER RA KHỎI HỆ THỐNG
        // =========================================================================
        String targetEmail = (String) data.get("email");

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            System.err.println("[AdminLetUserBeBanned] Thất bại: Thiếu thông tin email của user cần khóa!");
            ResponseSender.send(conn, responseType, "ERROR", "Email người dùng không hợp lệ!", null);
            return;
        }

        System.out.println("[AdminLetUserBeBanned] Admin [" + adminEmail + "] yêu cầu BAN tài khoản user: " + targetEmail);

        // Cập nhật trạng thái xuống Database
        boolean isDbUpdated = userDao.updateUserStatus(targetEmail, "BANNED");
        if (!isDbUpdated) {
            System.err.println("[AdminLetUserBeBanned] Thất bại: Không thể cập nhật trạng thái BANNED trong DB cho: " + targetEmail);
            ResponseSender.send(conn, responseType, "ERROR", "Không thể cập nhật trạng thái tài khoản trong cơ sở dữ liệu!", null);
            return;
        }

        // Tìm kết nối WebSocket (conn) của user bị ban trên RAM
        WebSocket targetConn = null;
        for (Map.Entry<WebSocket, User> entry : context.getOnlineUserObjects().entrySet()) {
            if (entry.getValue() != null && targetEmail.equalsIgnoreCase(entry.getValue().getEmail())) {
                targetConn = entry.getKey();
                break;
            }
        }

        // Gửi lệnh ép đăng xuất tới nạn nhân (nếu họ đang online)
        if (targetConn != null && targetConn.isOpen()) {
            context.updateUserStatusInRam(targetEmail, "BANNED");

            // Sử dụng luôn ResponseSender để gửi tín hiệu đá user khỏi hệ thống
            ResponseSender.send(
                    targetConn,
                    "ADMIN_FORCE_LOGOUT",
                    "SUCCESS",
                    "Tài khoản của bạn đã bị khóa (BANNED) bởi Admin!",
                    null
            );
            System.out.println("[AdminLetUserBeBanned] Đã gửi tín hiệu khóa tài khoản tới kết nối của: " + targetEmail);
        }

        // Tiến hành dọn dẹp kết nối ra khỏi RAM trên Server
        if (targetConn != null) {
            context.removeUser(targetConn);
            System.out.println("[AdminLetUserBeBanned] Đã gọi context.removeUser(targetConn)");
        }
        context.removeOnlineUserByEmail(targetEmail);
        System.out.println("[AdminLetUserBeBanned] Đã gọi context.removeOnlineUserByEmail(targetEmail)");

        // =========================================================================
        // 3. PHẢN HỒI THÔNG BÁO VỀ CHO ADMIN PHÁT LỆNH
        // =========================================================================
        // Giữ nguyên trạng thái "OTHER" theo cấu trúc logic cũ của bạn
        ResponseSender.send(conn, responseType, "OTHER", "Đã BAN người dùng có email: " + targetEmail, null);
    }
}