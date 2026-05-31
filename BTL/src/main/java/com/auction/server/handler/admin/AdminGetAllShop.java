package com.auction.server.handler.admin;

import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
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

@CommandMap("ADMIN_GET_ALL_SHOP")
public class AdminGetAllShop implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(AdminGetAllShop.class.getName());

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "ADMIN_GET_ALL_SHOP_RESPONSE");

        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có thẩm quyền truy cập dữ liệu này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        try {
            // ĐÃ TỐI ƯU: Gọi 1 query duy nhất lấy trọn vẹn data kèm count, giải quyết triệt để N+1 Query
            List<Map<String, Object>> shopList = userDao.getAllShopsWithProductCount();

            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Tải danh sách cửa hàng thành công!");

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("list", shopList);
            responseMap.put("data", dataMap);

            conn.send(gson.toJson(responseMap));

        } catch (Exception e) {
            LOGGER.severe("[AdminGetAllShop] Lỗi truy vấn Shop: " + e.getMessage());
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Lỗi máy chủ khi kết nối Database!");
            responseMap.put("data", new HashMap<>());
            conn.send(gson.toJson(responseMap));
        }
    }
}