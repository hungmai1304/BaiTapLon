package com.auction.server.handler;

import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@CommandMap("ADMIN_LOGOUT_COMMAND")
public class AdminLogoutCommand implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {

        // Tạo một Map để chuẩn bị phản hồi kết quả về cho Client
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("type", "ADMIN_LOUGOUT_COMMAND");

        // =========================================================================
        // 1. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
        // =========================================================================

        // Bước 1.1: Kiểm tra xem kết nối (conn) này đã đăng nhập vào hệ thống chưa
        String adminEmail = context.getUserByConn(conn);
        if (adminEmail == null) {
            System.err.println("[AdminLogoutCommand] Từ chối: Thao tác từ một kết nối chưa đăng nhập!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn cần đăng nhập để thực hiện thao tác này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // Bước 1.2: Lấy thông tin user từ DB để chắc chắn người này có quyền ADMIN
        UserDao userDao = UserDao.getInstance();
        User currentRequester = userDao.getUserByEmail(adminEmail);

        if (currentRequester == null || !"ADMIN".equalsIgnoreCase(currentRequester.getRole())) {
            System.err.println("[AdminLogoutCommand] Cảnh báo: Tài khoản " + adminEmail + " cố tình hack quyền Đăng xuất Admin!");
            responseMap.put("status", "ERROR");
            responseMap.put("message", "Bạn không có quyền thực hiện hành động này!");
            conn.send(gson.toJson(responseMap));
            return;
        }

        // =========================================================================
        // 2. XỬ LÝ ĐĂNG XUẤT (Cho email này rời khỏi cả 2 danh sách đang online)
        // =========================================================================
        System.out.println("[AdminLogoutCommand] Nhận lệnh đăng xuất từ Admin: " + adminEmail);

        // Gọi hàm xóa đồng bộ ở cả 2 danh sách (onlineUsers & onlineUserObjects) trong ServerContext
        context.removeOnlineUserByEmail(adminEmail);

        // Phản hồi gói tin SUCCESS về cho phía Client để họ chuyển hướng UI về màn hình Login
        responseMap.put("status", "SUCCESS");
        responseMap.put("message", "Đăng xuất tài khoản Admin thành công!");
        conn.send(gson.toJson(responseMap));
        // 1. Tìm các user trong database có role là ADMIN_REQUEST thông qua UserDao
        List<User> requestList = UserDao.getInstance().getUsersByRole("ADMIN_REQUEST");

        // 2. SỬA: Đóng gói dữ liệu chuẩn hóa qua lớp Response chuyên dụng
        Response response = new Response(
                MessageType.GET_ADMIN_REQUEST_LIST_RESPONSE,
                "SUCCESS",
                "Lấy danh sách thành công!"
        );

        // Bắt buộc phải đẩy vào bên trong map getData() thì Client mới bóc tách được!
        response.getData().put("users", requestList);

        // 3. Chuyển thành chuỗi JSON và gửi về cho Client qua WebSocket
        String jsonResponse = gson.toJson(response);
        if (conn != null && conn.isOpen()) {
            conn.send(jsonResponse);
            System.out.println("[Server Handler] Đã gửi danh sách phản hồi về Client (" + requestList.size() + " hàng).");
        }
    }
}