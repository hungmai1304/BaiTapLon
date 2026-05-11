package com.auction.server.handler;

import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.auction.server.dao.UserDao; // Import UserDao
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;


@CommandMap(value = MessageType.LOGIN_REQUEST)
public class LoginHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[LoginHandler] Đang xử lý đăng nhập...");

        try {

            String email = (String) data.get("email");
            String password = (String) data.get("password");
            System.out.println("Đang check Login cho: Email=" + email + " | Pass=" + password);
            // 2. LOGIC KIỂM TRA TÀI KHOẢN TỪ DATABASE
            // Gọi UserDao để check thông tin, nếu đúng sẽ trả về đối tượng User
            User loginUser = UserDao.getInstance().authenticate(email, password);

            if (loginUser != null) {
                // --- ĐĂNG NHẬP THÀNH CÔNG ---
                context.addOnlineUser(email, conn);

                // Khởi tạo Response
                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "SUCCESS",
                        "Đăng nhập thành công!"
                );

                // Nhét dữ liệu TỪ DATABASE vào Map data để gửi về cho Client
                response.getData().put("id", loginUser.getId());
                response.getData().put("email", loginUser.getEmail());
                response.getData().put("name", loginUser.getName());
                response.getData().put("role", loginUser.getRole());

                // Nếu User là Seller, có thể gửi thêm shopName về
                if (loginUser.getShopName() != null) {
                    response.getData().put("shopName", loginUser.getShopName());
                }

                // Gửi về cho Client
                conn.send(gson.toJson(response));
                System.out.println("[LoginHandler] Người dùng [" + loginUser.getName() + "] đã vào hệ thống.");

            } else {
                // --- ĐĂNG NHẬP THẤT BẠI ---
                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "ERROR",
                        "Sai tài khoản hoặc mật khẩu rồi!"
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