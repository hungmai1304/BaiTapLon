package com.auction.server.handler.admin;

import com.auction.common.model.user.User;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CommandMap("ADMIN_GET_BANNED_LIST")
public class AdminGetBannedList implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(AdminGetBannedList.class.getName());

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        // 1. Tạo Map tổng cho gói tin phản hồi
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "ADMIN_GET_BANNED_LIST_RESPONSE");

        // =========================================================================
        // KIỂM TRA CHÍNH CHỦ
        // =========================================================================
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            LOGGER.severe("[AdminGetBannedList] Từ chối: Kết nối chưa đăng nhập!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            LOGGER.severe("[AdminGetBannedList] Cảnh báo: " + adminEmail + " hack quyền xem danh sách BAN!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ LẤY DANH SÁCH BANNED & BLACKLIST VÀ GỘP LẠI
        // =========================================================================
        LOGGER.info("[AdminGetBannedList] Admin [" + adminEmail + "] đang yêu cầu lấy danh sách tài khoản bị hạn chế (BANNED & BLACKLIST).");

        try {
            // Lấy danh sách tài khoản BANNED từ DB
            List<User> restrictedUsers = userDao.getUsersByStatus("BANNED");

            // THÊM MỚI: Lấy thêm danh sách tài khoản BLACKLIST từ DB
            List<User> blacklistUsers = userDao.getUsersByStatus("BLACKLIST");

            // Gộp danh sách tài khoản BLACKLIST vào chung danh sách kết quả
            if (blacklistUsers != null && !blacklistUsers.isEmpty()) {
                restrictedUsers.addAll(blacklistUsers);
            }

            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Lấy danh sách tài khoản bị hạn chế thành công!");

            // GIẢI PHÁP TẬN GỐC: Tạo một Map con đại diện cho trường "data"
            Map<String, Object> dataMap = new HashMap<>();

            // Nhét cái List đã gộp vào trong Map con này thông qua key "list"
            dataMap.put("list", restrictedUsers);

            // Đưa Map con vào Map tổng. Lúc này data hoàn toàn là một JSON Object { } đúng ý Client!
            responseMap.put("data", dataMap);

            // Tiến hành gửi chuỗi JSON đi
            conn.send(gson.toJson(responseMap));
            LOGGER.info("[AdminGetBannedList] Đã gửi thành công tổng cộng " + restrictedUsers.size() + " tài khoản (BANNED/BLACKLIST) cho Admin.");

        } catch (Exception e) {
            LOGGER.severe("[AdminGetBannedList] Lỗi hệ thống: " + e.getMessage());

            responseMap.put("status", "ERROR");
            responseMap.put("message", "Có lỗi xảy ra khi truy vấn dữ liệu từ Server!");
            responseMap.put("data", new HashMap<>()); // Trả về map rỗng chứ không trả về mảng null
            conn.send(gson.toJson(responseMap));
        }
    }
}