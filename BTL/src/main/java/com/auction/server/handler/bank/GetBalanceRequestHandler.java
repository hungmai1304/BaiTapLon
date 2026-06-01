package com.auction.server.handler.bank;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.logging.Logger;

// Đăng ký Handler với MessageType tương ứng cho tính năng xem số dư
@CommandMap(value = MessageType.GET_BALANCE_REQUEST)
public class GetBalanceRequestHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(GetBalanceRequestHandler.class.getName());

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        LOGGER.info("[GetBalanceHandler] Đang xử lý yêu cầu kiểm tra số dư...");

        try {
            // 1. Kiểm tra trạng thái đăng nhập từ Connection Session
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập để kiểm tra số dư!");
                return;
            }

            // 2. Truy vấn trực tiếp số dư hiện tại từ Database thông qua hàm có sẵn trong UserDao
            double currentBalance = UserDao.getInstance().getBalanceByEmail(userEmail);

            // 3. Đóng gói gói tin phản hồi trả lại cho Client
            Response response = new Response(
                    MessageType.GET_BALANCE_RESPONSE,
                    "SUCCESS",
                    "Lấy thông tin số dư tài khoản thành công."
            );

            // Đưa dữ liệu số dư vào map data để Client hiển thị lên giao diện UI
            response.getData().put("balance", currentBalance);

            // 4. Gửi dữ liệu qua WebSocket dưới dạng JSON
            conn.send(gson.toJson(response));
            LOGGER.info("[GetBalanceHandler] Đã gửi số dư cho [" + userEmail + "]: " + currentBalance);

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống khi lấy số dư: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.GET_BALANCE_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}