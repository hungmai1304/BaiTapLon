package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.auction.common.model.user.User;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

@CommandMap(value = MessageType.WITHDRAW_REQUEST)
public class WithdrawHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[WithdrawHandler] Đang xử lý yêu cầu rút tiền...");

        try {
            // 1. Kiểm tra trạng thái đăng nhập
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập để thực hiện tính năng này!");
                return;
            }

            // 2. Kiểm tra dữ liệu đầu vào số tiền
            if (!data.containsKey("data")) {
                sendError(conn, gson, "Dữ liệu yêu cầu không hợp lệ!");
                return;
            }

            Object amountObj = data.get("data");
            if (amountObj == null) {
                sendError(conn, gson, "Số tiền rút không được để trống!");
                return;
            }

            double amount = ((Number) amountObj).doubleValue();
            if (amount <= 0) {
                sendError(conn, gson, "Số tiền rút phải lớn hơn 0!");
                return;
            }

            // Lấy thông tin user hiện tại để kiểm tra nhanh trên RAM/DB trước khi chạy câu lệnh SQL
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);
            if (currentUser != null && currentUser.getBalance() < amount) {
                sendError(conn, gson, "Số dư tài khoản không đủ để thực hiện giao dịch này! (Hiện có: " + currentUser.getBalance() + ")");
                return;
            }

            // 3. Gọi DAO xử lý trừ tiền trong DB
            boolean isUpdated = UserDao.getInstance().withdrawMoney(userEmail, amount);

            if (isUpdated) {
                // 4. Lấy lại dữ liệu ví mới nhất để đồng bộ về Client
                User updatedUser = UserDao.getInstance().getUserByEmail(userEmail);

                Response response = new Response(
                        MessageType.WITHDRAW_RESPONSE,
                        "SUCCESS",
                        "Rút tiền thành công! Số dư còn lại của bạn là: " + updatedUser.getBalance()
                );

                // Đóng gói số dư mới trả về cho Client JavaFX cập nhật lại giao diện UI hiển thị ngân hàng
                response.getData().put("newBalance", updatedUser.getBalance());

                conn.send(gson.toJson(response));
                System.out.println("[WithdrawHandler] Rút tiền thành công cho " + userEmail + ": -" + amount);
            } else {
                sendError(conn, gson, "Rút tiền thất bại! Vui lòng kiểm tra lại tài khoản.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống khi rút tiền: " + e.getMessage());
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.WITHDRAW_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}