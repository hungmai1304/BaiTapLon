package com.auction.server.handler.login;

import com.auction.common.model.user.Admin;
import com.auction.common.model.user.Seller;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
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

                // Kiểm tra bằng instanceof để chắc chắn class khởi tạo là Admin đặc quyền
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
                    response.getData().put("avatar", admin.getAvatar());
                    response.getData().put("status", admin.getStatus());

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

            // --- KIỂM TRA TRẠNG THÁI BANNED TRƯỚC KHI ĐĂNG NHẬP ---
            User userCheck = UserDao.getInstance().getUserByEmail(email);
            if (userCheck != null && "BANNED".equalsIgnoreCase(userCheck.getStatus())) {
                System.err.println("[LoginHandler] Từ chối xác thực: Tài khoản [" + email + "] đang bị khóa (BANNED)!");
                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "ERROR",
                        "Bạn đã bị kick!"
                );
                conn.send(gson.toJson(response));
                return;
            }

            // --- XỬ LÝ USER THƯỜNG / SELLER QUA DATABASE ---
            User loginUser = UserDao.getInstance().authenticate(email, password);

            if (loginUser != null) {
                context.addOnlineUser(email, conn);
                context.addOnlineUserObject(conn, loginUser);

                // Mặc định lời nhắn thành công
                String welcomeMessage = "Đăng nhập thành công!";

                // THÊM MỚI: Nếu đăng nhập thành công nhưng tài khoản thuộc BLACKLIST, thay đổi message cảnh báo
                if ("BLACKLIST".equalsIgnoreCase(loginUser.getStatus())) {
                    welcomeMessage = "Lưu ý: Bạn đang trong Blacklist!";
                    System.out.println("[LoginHandler] Cảnh báo: Thành viên nhóm danh sách đen [" + loginUser.getEmail() + "] vừa đăng nhập.");
                }

                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "SUCCESS",
                        welcomeMessage
                );

                // 1. Nhặt dữ liệu chung
                response.getData().put("id", loginUser.getId());
                response.getData().put("email", loginUser.getEmail());
                response.getData().put("name", loginUser.getUsername());
                response.getData().put("avatar", loginUser.getAvatar());
                response.getData().put("role", loginUser.getRole());
                response.getData().put("status", loginUser.getStatus());

                // 2. Bổ sung thuộc tính mở rộng chuyên biệt (như shopName) nếu là Seller
                if (loginUser instanceof Seller) {
                    response.getData().put("shopName", ((Seller) loginUser).getShopName());
                }

                conn.send(gson.toJson(response));
                System.out.println("[LoginHandler] User [" + loginUser.getUsername() + "] với vai trò [" + loginUser.getRole() + "] và trạng thái [" + loginUser.getStatus() + "] đã vào hệ thống.");

            } else {
                // Đăng nhập thất bại do sai tài khoản hoặc mật khẩu thông thường
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