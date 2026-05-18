package com.auction.server.handler;

import com.auction.common.model.user.Admin; // Import class Admin của bạn
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

                // 1. Kiểm tra email và mật khẩu dưới Database
                User maybeAdmin = UserDao.getInstance().authenticate(email, password);

                // Kiểm tra xem tài khoản có tồn tại và thực sự có Role/Class là Admin hay không
                if (maybeAdmin instanceof Admin) {
                    Admin admin = (Admin) maybeAdmin;

                    // 2. Cho Admin online trong ServerContext dưới dạng một User thông thường
                    context.addOnlineUser(email, conn);

                    // 3. Đóng gói tin nhắn trả về ADMIN_LOGIN_RESPONSE
                    Response response = new Response(
                            MessageType.ADMIN_LOGIN_RESPONSE,
                            "SUCCESS",
                            "Admin đăng nhập thành công!"
                    );

                    // 4. Đóng gói thông tin dữ liệu chuyển về Client
                    response.getData().put("id", admin.getId());
                    response.getData().put("email", admin.getEmail());
                    response.getData().put("name", admin.getUsername());
                    response.getData().put("role", "ADMIN");

                    conn.send(gson.toJson(response));
                    System.out.println("[LoginHandler] Admin [" + admin.getUsername() + "] đã vào hệ thống.");
                    return; // Kết thúc hàm, không chạy xuống logic User thường ở dưới nữa
                } else {
                    // Nếu chứa đuôi @admin.com nhưng sai mật khẩu hoặc phân quyền trong DB không phải Admin
                    System.out.println("[LoginHandler] Xác thực Admin thất bại!");
                    Response response = new Response(
                            MessageType.ADMIN_LOGIN_RESPONSE, // Hoặc dùng chung LOGIN_RESPONSE tùy thiết kế hệ thống
                            "ERROR",
                            "Tài khoản Admin không hợp lệ hoặc sai mật khẩu!"
                    );
                    conn.send(gson.toJson(response));
                    return;
                }
            }


            // --- XỬ LÝ USER THƯỜNG QUA DATABASE ---
            // 1. Kiểm tra đăng nhập từ Database
            User loginUser = UserDao.getInstance().authenticate(email, password);

            if (loginUser != null) {
                // --- ĐĂNG NHẬP THÀNH CÔNG ---
                context.addOnlineUser(email, conn);

                // VIẾT THÊM: Lưu thông tin đối tượng User vào RAM và tự động thông báo tới Admin
                context.addOnlineUserObject(conn, loginUser);

                Response response = new Response(
                        MessageType.LOGIN_RESPONSE,
                        "SUCCESS",
                        "Đăng nhập thành công!"
                );

                // 2. Nhặt dữ liệu chung
                response.getData().put("id", loginUser.getId());
                response.getData().put("email", loginUser.getEmail());
                response.getData().put("name", loginUser.getUsername());

                // 3. Kiểm tra Role bằng instanceof
                if (loginUser instanceof Seller) {
                    response.getData().put("role", "SELLER");
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