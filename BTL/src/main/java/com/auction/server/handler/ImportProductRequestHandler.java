package com.auction.server.handler;


import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

@CommandMap(value = MessageType.IMPORT_PRODUCT_REQUEST)
public class ImportProductRequestHandler {
    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. LẤY THÔNG TIN NGƯỜI GỬI (OWNER) TỪ SERVER CONTEXT
            // Giả sử context có hàm lấy email hoặc User object dựa trên WebSocket connection
            String userEmail = ServerContext.getUserByConn(WebSocket conn);
            if (userEmail == null) {
                sendError(conn, gson, "Bạn cần đăng nhập để thực hiện chức năng này!");
                return;
            }

            // Truy xuất thông tin User đầy đủ từ Database hoặc Cache
            User currentUser = UserDao.getInstance().getUserByEmail(userEmail);

            // 2. LẤY DỮ LIỆU SẢN PHẨM TỪ CLIENT
            String name = (String) data.get("name");
            // ... (các trường khác như id, category, price tương tự bài trước)

            // 3. TẠO ĐỐI TƯỢNG PRODUCT VÀ GÁN OWNER
            Product product = new Product();
            product.setId((String) data.get("id"));
            product.setName(name);
            // ...

            // GÁN OWNER CHÍNH LÀ NGƯỜI ĐANG KẾT NỐI
            product.setOwner(currentUser);

            // 4. LƯU VÀO DATABASE
            boolean isSaved = ProductDao.getInstance().saveProduct(product);

            if (isSaved) {
                Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "SUCCESS", "Đã lưu!");
                conn.send(gson.toJson(response));
            }

        } catch (Exception e) {
            e.printStackTrace();
            sendError(conn, gson, "Lỗi hệ thống!");
        }
    }

    private void sendError(WebSocket conn, Gson gson, String msg) {
        Response response = new Response(MessageType.IMPORT_PRODUCT_RESPONSE, "ERROR", msg);
        conn.send(gson.toJson(response));
    }
}
