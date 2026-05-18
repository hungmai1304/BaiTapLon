package com.auction.server.handler;

import com.auction.common.model.user.Admin;
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

            // --- XỬ LÝ ĐĂNG NHẬP CHO ADMIN ---
            if (email != null && email.toLowerCase().endsWith("@admin.com")) {
                System.out.println("[LoginHandler] Phát hiện định dạng Email Admin. Tiến hành xác thực...");

                User maybeAdmin = UserDao.getInstance().authenticate(email, password);

                // Kiểm tra bằng instanceof để chắc chắn class khởi tạo là Admin độc quyền
                if (maybeAdmin instanceof Admin) {
                    Admin admin = (Admin) maybeAdmin;

                    context.addOnlineUser(email, conn);
                    context.addOnlineUserObject(conn, admin);

                    Response response = new Response(
                            MessageType.ADMIN_LOGIN_RESPONSE,
                            "SUCCESS",
                            "Admin đăng nhập thành công!"
                    );

                    response.getData().put("id", admin.getId());
                    response.getData().put("email", admin.getEmail());
                    response.getData().put("name", admin.getUsername());
                    response.getData().put("role", "ADMIN");

                    conn.send(gson.toJson(response));
                    System.out.println("[LoginHandler] Admin [" + admin.getUsername() + "] đã vào hệ thống.");
                    return;
                } else {
                    System.out.println("[LoginHandler] Xác thực Admin thất bại!");
                    Response response = new Response(
                            MessageType.ADMIN_LOGIN_RESPONSE,
                            "ERROR",
                            "Tài khoản Admin không hợp lệ hoặc sai mật khẩu!"
                    );
                    conn.send(gson.toJson(response));
                    return;
                }
            }

            // --- XỬ LÝ USER THƯỜNG / ADMIN_REQUEST QUA DATABASE ---
            User loginUser = UserDao.getInstance().authenticate(email, password);

            if (loginUser != null) {
                context.addOnlineUser(email, conn);
                context.addOnlineUserObject(conn, loginUser);

                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "SUCCESS",
                        "Đăng nhập thành công!"
                );

                // 1. Nhặt dữ liệu chung
                response.getData().put("id", loginUser.getId());
                response.getData().put("email", loginUser.getEmail());
                response.getData().put("name", loginUser.getUsername());

                // ĐỒNG BỘ MỚI: Lấy trực tiếp role từ thuộc tính của đối tượng thay vì đoán qua instanceof
                response.getData().put("role", loginUser.getRole());

                // 2. Chỉ dùng instanceof để bổ sung các thuộc tính mở rộng chuyên biệt (như shopName)
                if (loginUser instanceof Seller) {
                    response.getData().put("shopName", ((Seller) loginUser).getShopName());
                }

                conn.send(gson.toJson(response));
                System.out.println("[LoginHandler] User [" + loginUser.getUsername() + "] với vai trò [" + loginUser.getRole() + "] đã vào hệ thống.");

            } else {
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