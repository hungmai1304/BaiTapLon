package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

// Đánh dấu để MessageDispatcher tìm thấy khi quét radar
@CommandMap(value = MessageType.LOGIN_REQUEST)
public class LoginHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[LoginHandler] Đang xử lý đăng nhập...");

        try {
            // 1. Lấy dữ liệu từ Client gửi lên
            String username = (String) data.get("username");
            String password = (String) data.get("password");

            // 2. LOGIC KIỂM TRA TÀI KHOẢN (Theo sơ đồ: Logic check username vs password)
            // (Tạm thời mình Hardcode để test trước, sau này nối Database (UserDao) vào đây sau nhé)

             // Mai kết nối database sẽ xóa sau

            // 2. Kiểm tra logic tài khoản
            if ("kietva@gmail.com".equals(username) && "123456".equals(password)) {

                // --- ĐĂNG NHẬP THÀNH CÔNG ---
                context.addOnlineUser(username, conn);

                // Khởi tạo Response ĐÚNG chuẩn 3 tham số
                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "SUCCESS",
                        "Đăng nhập thành công!"
                );

                // Nhét dữ liệu vào Map data thông qua hàm getData()
                response.getData().put("username", username);
                response.getData().put("name", "Vũ Anh Kiệt");
                response.getData().put("role", "ADMIN");
                response.getData().put("balance", 5000000.0);

                // Gửi về cho Client
                conn.send(gson.toJson(response));
                System.out.println("[LoginHandler] Người dùng [" + username + "] đã vào hệ thống.");

            } else {
                // --- ĐĂNG NHẬP THẤT BẠI ---
                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "ERROR",
                        "Sai tài khoản hoặc mật khẩu rồi anh trai ơi!"
                );

                conn.send(gson.toJson(response));
                System.out.println("[LoginHandler] Khách hàng nhập sai thông tin!");
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Trả về lỗi hệ thống nếu có biến cố
            Response response = new Response(
                    MessageType.LOGIN_RESPONSE,
                    "ERROR",
                    "Lỗi Server: " + e.getMessage()
            );
            conn.send(gson.toJson(response));
        }
    }
}