package com.auction.server.handler.admin;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.user.User;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandMap("ADMIN_GET_ONLINE_AUCTIONS")
public class AdminGetOnlineAuctionsRequest implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "ADMIN_GET_ONLINE_AUCTIONS_RESPONSE");

        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Vui lòng đăng nhập quyền Admin!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Từ chối truy cập: Quyền hạn không hợp lệ!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        try {
            List<Auction> rawActiveAuctions;

            // TỐI ƯU KHÓA (Lock Minimization): Chỉ lock đúng thao tác clone mảng gốc rồi giải phóng ngay lập tức (mất < 1 mili-giây)
            synchronized (context.getActiveAuctions()) {
                rawActiveAuctions = new ArrayList<>(context.getActiveAuctions());
            }

            List<Map<String, Object>> formattedAuctions = new ArrayList<>();

            // Việc loop bóc tách dữ liệu nặng nề giờ đây hoàn toàn nằm ngoài block synchronized, không làm nghẽn người dùng khác đặt giá
            for (Auction auction : rawActiveAuctions) {
                if (auction == null) continue;

                Map<String, Object> flatItem = new HashMap<>();
                flatItem.put("id", auction.getId());

                if (auction.getProduct() != null) {
                    flatItem.put("productName", auction.getProduct().getName());
                    flatItem.put("status", auction.getProduct().getStatus());

                    if (auction.getProduct().getOwner() != null) {
                        flatItem.put("ownerEmail", auction.getProduct().getOwner().getEmail());
                    } else {
                        flatItem.put("ownerEmail", "Ẩn danh (N/A)");
                    }
                } else {
                    flatItem.put("productName", "Sản phẩm không xác định");
                    flatItem.put("status", "UNKNOWN");
                    flatItem.put("ownerEmail", "N/A");
                }

                formattedAuctions.add(flatItem);
            }

            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Tải danh sách phiên trực tuyến thành công!");

            Map<String, Object> dataMap = new HashMap<>();
            dataMap.put("list", formattedAuctions);
            responseMap.put("data", dataMap);

            conn.send(gson.toJson(responseMap));

        } catch (Exception e) {
            System.err.println("[AdminGetOnlineAuctions] Lỗi bóc tách RAM: " + e.getMessage());
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Có lỗi xảy ra khi xử lý dữ liệu hệ thống!");
            responseMap.put("data", new HashMap<>());
            conn.send(gson.toJson(responseMap));
        }
    }
}