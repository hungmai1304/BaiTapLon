package com.auction.server.handler;

import com.auction.protocol.MessageType;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandMap(value = MessageType.GET_SHOP_PRODUCTS_REQUEST)
public class GetShopProductsHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            // 1. XÁC NHẬN THÔNG QUA CONN
            // Lấy email của người dùng đang kết nối từ context
            String userEmail = context.getUserByConn(conn);

            if (userEmail == null) {
                System.err.println("[GetShopProductsHandler] Cảnh báo: Truy cập trái phép từ " + conn.getRemoteSocketAddress());
                Response errorResponse = new Response(MessageType.GET_SHOP_PRODUCTS_RESPONSE, "ERROR", "Bạn cần đăng nhập để xem danh sách sản phẩm!");
                conn.send(gson.toJson(errorResponse));
                return;
            }

            System.out.println("[GetShopProductsHandler] User: " + userEmail + " đang lấy danh sách shop...");

            // 2. Lấy filter status nếu có (từ request data)
            String statusFilter = (String) data.get("status");

            // 3. Lấy dữ liệu từ Database dựa trên userEmail vừa xác nhận
            // Sử dụng userEmail lấy từ conn để đảm bảo an toàn, không sợ bị fake sellerId từ client
            List<Product> rawList = ProductDao.getInstance().getProductsByUserEmail(userEmail);
            List<Product> result;

            if (statusFilter != null && !statusFilter.isEmpty()) {
                result = rawList.stream()
                        .filter(p -> p.getStatus().name().equalsIgnoreCase(statusFilter))
                        .collect(Collectors.toList());
            } else {
                result = rawList;
            }

            // 4. Đóng gói response
            Response response = new Response(
                    MessageType.GET_SHOP_PRODUCTS_RESPONSE,
                    "SUCCESS",
                    "Lấy danh sách sản phẩm shop thành công"
            );
            response.getData().put("products", result);

            // Gửi dữ liệu về (chỉ chứa link URL ảnh, cực nhẹ)
            conn.send(gson.toJson(response));

            System.out.println("[GetShopProductsHandler] Đã gửi " + result.size() + " sản phẩm cho shop của " + userEmail);

        } catch (Exception e) {
            System.err.println("[GetShopProductsHandler] Lỗi: " + e.getMessage());
            e.printStackTrace();
            conn.send(gson.toJson(new Response(MessageType.GET_SHOP_PRODUCTS_RESPONSE, "ERROR", "Lỗi hệ thống: " + e.getMessage())));
        }
    }
}