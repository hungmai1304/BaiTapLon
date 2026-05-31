    package com.auction.server.handler.admin;

    import com.auction.server.annotation.CommandMap;
    import com.auction.server.handler.IMessageHandler;
    import com.auction.server.model.ServerContext;
    import com.auction.common.model.user.User;
    import com.auction.server.dao.UserDao;
    import com.google.gson.Gson;
    import org.java_websocket.WebSocket;

    import java.util.HashMap;
    import java.util.Map;
    import java.util.logging.Logger;

    @CommandMap("BACK_TO_ADMIN_COMMAND")
    public class BackToAdminHandler implements IMessageHandler {
        private static final Logger LOGGER = Logger.getLogger(BackToAdminHandler.class.getName());

        @Override
        public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("type", "BACK_TO_ADMIN_RESPONSE");

            // =========================================================================
            // 1. KIỂM TRA DỮ LIỆU ĐẦU VÀO (Request Validation)
            // =========================================================================
            String clientProvidedEmail = (String) data.get("email");

            if (clientProvidedEmail == null || clientProvidedEmail.trim().isEmpty()) {
                LOGGER.severe("[BackToAdminHandler] Từ chối: Thiếu thông tin Email định danh từ Client!");
                responseMap.put("status", "ERROR");
                responseMap.put("message", "Dữ liệu yêu cầu không hợp lệ!");
                conn.send(gson.toJson(responseMap));
                return;
            }

            // =========================================================================
            // 2. KIỂM TRA CHÍNH CHỦ (Authentication & Authorization)
            // =========================================================================

            // Bước 2.1: Kiểm tra xem kết nối mạng đã từng đăng nhập chưa
            String loggedInEmail = context.getUserByConn(conn);
            if (loggedInEmail == null) {
                LOGGER.severe("[BackToAdminHandler] Từ chối: Kết nối mạng này chưa từng đăng ký Session đăng nhập!");
                responseMap.put("status", "ERROR");
                responseMap.put("message", "Bạn cần đăng nhập lại để thực hiện thao tác này!");
                conn.send(gson.toJson(responseMap));
                return;
            }

            // Bước 2.2: Cross-check chống giả mạo gói tin
            if (!loggedInEmail.equalsIgnoreCase(clientProvidedEmail.trim())) {
                LOGGER.severe("[BackToAdminHandler] Cảnh báo: Email kết nối mạng ('" + loggedInEmail + "') lệch với Email client gửi lên ('" + clientProvidedEmail + "')!");
                responseMap.put("status", "ERROR");
                responseMap.put("message", "Xác thực danh tính thất bại!");
                conn.send(gson.toJson(responseMap));
                return;
            }

            // Bước 2.3: Kiểm tra quyền ADMIN từ Database công tâm
            UserDao userDao = UserDao.getInstance();
            User currentRequester = userDao.getUserByEmail(loggedInEmail);

            // Trường hợp lỗi 1: Không tìm thấy User trong Database
            if (currentRequester == null) {
                LOGGER.severe("[BackToAdminHandler] LỖI hệ thống: Không tìm thấy tài khoản tương ứng với email '" + loggedInEmail + "' trong Database!");
                responseMap.put("status", "ERROR");
                responseMap.put("message", "Tài khoản của bạn không tồn tại trên hệ thống dữ liệu!");
                conn.send(gson.toJson(responseMap));
                return;
            }

            // GIẢI PHÁP CHỐNG LỖI ENUM & KHOẢNG TRẮNG:
            // Ép dữ liệu getRole() về dạng String thuần túy, loại bỏ khoảng trắng thừa
            String roleStr = "";
            if (currentRequester.getRole() != null) {
                roleStr = currentRequester.getRole().toString().trim();
            }

            // Log cực kỳ quan trọng này sẽ cho bạn biết Server thực tế đang đọc ra chữ gì từ DB
            LOGGER.info("[DEBUG - SERVER LOG] Tài khoản: '" + loggedInEmail + "' | Quyền hạn thực tế đọc từ DB: '" + roleStr + "'");

            if (!"ADMIN".equalsIgnoreCase(roleStr)) {
                LOGGER.severe("[BackToAdminHandler] Bị từ chối: Tài khoản '" + loggedInEmail + "' mang quyền '" + roleStr + "' chứ không phải ADMIN!");
                responseMap.put("status", "ERROR");
                responseMap.put("message", "Bạn không có quyền truy cập vào giao diện quản trị!");
                conn.send(gson.toJson(responseMap));
                return;
            }

            // =========================================================================
            // 3. PHẢN HỒI THÀNH CÔNG (Success Response)
            // =========================================================================
            LOGGER.info("[BackToAdminHandler] Xác thực THÀNH CÔNG! Cho phép Admin " + loggedInEmail + " chuyển cảnh.");

            responseMap.put("status", "SUCCESS");
            responseMap.put("message", "Xác thực Admin thành công!");
            conn.send(gson.toJson(responseMap));
        }
    }