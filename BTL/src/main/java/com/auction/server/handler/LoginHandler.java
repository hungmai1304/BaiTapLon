package com.auction.server.handler;

import com.auction.common.model.user.Bidder;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.auction.server.dao.UserDao;
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

            // 1. Kiểm tra đăng nhập từ Database
            User loginUser = UserDao.getInstance().authenticate(email, password);

            if (loginUser != null) {
                // --- ĐĂNG NHẬP THÀNH CÔNG ---
                context.addOnlineUser(email, conn);

                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "SUCCESS",
                        "Đăng nhập thành công!"
                );

                // 2. Nhét dữ liệu chung
                response.getData().put("id", loginUser.getId());
                response.getData().put("email", loginUser.getEmail());
                response.getData().put("name", loginUser.getUsername());

                // 3. Kiểm tra Role bằng instanceof (Thay vì dùng loginUser.getRole())
                if (loginUser instanceof Seller) {
                    response.getData().put("role", "SELLER");
                    // Lấy shopName từ đối tượng Seller
                    response.getData().put("shopName", ((Seller) loginUser).getShopName());
                } else if (loginUser instanceof Bidder) {
                    response.getData().put("role", "BIDDER");
                }

                conn.send(gson.toJson(response));
                System.out.println("[LoginHandler] User [" + loginUser.getUsername() + "] đã vào hệ thống.");

            } else {
                // --- ĐĂNG NHẬP THẤT BẠI ---
                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "ERROR",
                        "Sai tài khoản hoặc mật khẩu!"
                );
                conn.send(gson.toJson(response));
            }

        } catch (Exception e) {
            e.printStackTrace();
            Response response = new Response(
                    MessageType.LOGIN_RESPONSE,
                    "ERROR",
                    "Lỗi Server: " + e.getMessage()
            );
            conn.send(gson.toJson(response));
        }
    }
}