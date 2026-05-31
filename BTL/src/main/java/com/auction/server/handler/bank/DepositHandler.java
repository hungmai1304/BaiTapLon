package com.auction.server.handler.bank;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.auction.common.model.user.User;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;
import java.util.logging.Logger;

@CommandMap(value = MessageType.DEPOSIT_REQUEST)
public class DepositHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(DepositHandler.class.getName());

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        LOGGER.info("[DepositHandler] Đang xử lý yêu cầu nạp tiền...");

        try {
            // 1. Kiểm tra đăng nhập (Lấy email người dùng từ connection session)
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập để thực hiện tính năng này!");
                return;
            }

            // 2. Lấy số tiền cần nạp từ dữ liệu Client gửi lên
            // Vì Gson có thể parse số thành kiểu Double/Number nên ta ép kiểu an toàn qua Number
            if (!data.containsKey("data")) { // Hoặc tùy thuộc vào cấu trúc map bọc data của bạn
                sendError(conn, gson, "Dữ liệu yêu cầu không hợp lệ!");
                return;
            }

            Object amountObj = data.get("data");
            if (amountObj == null) {
                sendError(conn, gson, "Số tiền không được để trống!");
                return;
            }

            double amount = ((Number) amountObj).doubleValue();
            if (amount <= 0) {
                sendError(conn, gson, "Số tiền nạp phải lớn hơn 0!");
                return;
            }

            // 3. Thực hiện cập nhật vào Database
            boolean isUpdated = UserDao.getInstance().depositMoney(userEmail, amount);

            if (isUpdated) {
                // 4. Đồng bộ lại dữ liệu mới nhất để gửi trả về cho Client hiển thị lên UI
                User updatedUser = UserDao.getInstance().getUserByEmail(userEmail);

                // (Nếu ServerContext của bạn có quản lý lưu trữ Object User trong RAM, hãy cập nhật nó tại đây)
                // context.updateUser(updatedUser);

                Response response = new Response(
                        MessageType.DEPOSIT_RESPONSE, // Đảm bảo bạn có trường này trong MessageType enum
                        "SUCCESS",
                        "Nạp tiền thành công! Số dư mới của bạn là: " + updatedUser.getBalance()
                );

                // Trả về số dư mới để Client cập nhật trực tiếp lên giao diện UI Bank
                response.getData().put("newBalance", updatedUser.getBalance());

                conn.send(gson.toJson(response));
                LOGGER.info("[DepositHandler] Nạp tiền thành công cho " + userEmail + ": +" + amount);
            } else {
                sendError(conn, gson, "Không thể cập nhật số dư vào hệ thống!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống khi nạp tiền: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        // Thay bằng MessageType.DEPOSIT_RESPONSE hoặc MESSAGE_ERROR tùy thiết kế mẫu của bạn
        Response response = new Response(MessageType.DEPOSIT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}