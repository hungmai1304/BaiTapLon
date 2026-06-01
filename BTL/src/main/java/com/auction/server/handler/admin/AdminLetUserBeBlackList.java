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
import java.util.logging.Logger;

@CommandMap("ADMIN_LET_USER_BE_BLACKLIST") // Đặt annotation trùng khớp chính xác với chuỗi client gửi lên
public class AdminLetUserBeBlackList implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(AdminLetUserBeBlackList.class.getName());

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        String responseType = "ADMIN_LET_USER_BE_BLACLIST_RESPONSE";

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            LOGGER.severe("[AdminLetUserBeBlackList] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            ResponseSender.send(conn, responseType, "ERROR", "Bạn cần đăng nhập để thực hiện thao tác này!", null);
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            LOGGER.severe("[AdminLetUserBeBlackList] Cảnh báo: Tài khoản " + adminEmail + " cố tình hack quyền BLACKLIST!");
            ResponseSender.send(conn, responseType, "ERROR", "Bạn không có quyền thực hiện hành động này!", null);
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ CẬP NHẬT TRẠNG THÁI BLACKLIST TRONG DATABASE
        // =========================================================================
        String targetEmail = (String) data.get("email");

        if (targetEmail == null || targetEmail.trim().isEmpty()) {
            LOGGER.severe("[AdminLetUserBeBlackList] Thất bại: Thiếu thông tin email của user cần đưa vào danh sách đen!");
            ResponseSender.send(conn, responseType, "ERROR", "Email người dùng không hợp lệ!", null);
            return;
        }

        LOGGER.info("[AdminLetUserBeBlackList] Admin [" + adminEmail + "] yêu cầu BLACKLIST tài khoản user: " + targetEmail);

        // Chỉ cập nhật trạng thái xuống Database thành "BLACKLIST" (Không kick out user)
        boolean isDbUpdated = userDao.updateUserStatus(targetEmail, "BLACKLIST");
        if (!isDbUpdated) {
            LOGGER.severe("[AdminLetUserBeBlackList] Thất bại: Không thể cập nhật trạng thái BLACKLIST trong DB cho: " + targetEmail);
            ResponseSender.send(conn, responseType, "ERROR", "Không thể cập nhật trạng thái tài khoản sang danh sách đen trong cơ sở dữ liệu!", null);
            return;
        }

        // =========================================================================
        // 3. PHẢN HỒI THÔNG BÁO VỀ CHO ADMIN PHÁT LỆNH
        // =========================================================================
        ResponseSender.send(conn, responseType, "OTHER", "Đã đưa người dùng có email " + targetEmail + " vào BLACKLIST thành công!", null);
    }
}