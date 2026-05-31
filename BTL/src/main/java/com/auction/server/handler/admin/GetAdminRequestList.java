package com.auction.server.handler.admin;

import com.auction.server.dao.UserDao;
import com.auction.common.model.user.User;
import com.auction.common.model.user.Admin;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response; // Import class Response chuẩn của hệ thống vào đây
import com.auction.server.annotation.CommandMap;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CommandMap(MessageType.GET_ADMIN_REQUEST_LIST)
public class GetAdminRequestList implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(GetAdminRequestList.class.getName());
    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        LOGGER.info("[Server Handler] Đang xử lý yêu cầu lấy danh sách xin làm Admin...");

        // XÁC MINH DANH TÍNH NGƯỜI GỬI
        User currentUser = context.getUserByConnObject(conn);

        // Kiểm tra xem đối tượng có phải là một thực thể của lớp Admin hay không
        if (!(currentUser instanceof Admin)) {
            LOGGER.severe("[CẢNH BÁO] Kết nối chưa xác thực hoặc không phải đối tượng Admin cố gắng truy cập dữ liệu!");

            // SỬA: Sử dụng cấu trúc Response chuẩn thay vì Map thủ công
            Response errorResponse = new Response(
                    MessageType.GET_ADMIN_REQUEST_LIST_RESPONSE, // Hoặc dùng "OTHER" nếu bạn muốn giữ nguyên kiểu cũ
                    "ERROR",
                    "Bạn cần đăng nhập bằng tài khoản Admin để thực hiện thao tác này!"
            );

            if (conn != null && conn.isOpen()) {
                conn.send(gson.toJson(errorResponse));
            }
            return;
        }

        // NẾU XÁC THỰC THÀNH CÔNG -> TIẾP TỤC XỬ LÝ LẤY DỮ LIỆU
        LOGGER.info("[Server Handler] Đã xác thực thành công Admin: " + currentUser.getUsername());

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
            LOGGER.info("[Server Handler] Đã gửi danh sách phản hồi về Client (" + requestList.size() + " hàng).");
        }
    }
}