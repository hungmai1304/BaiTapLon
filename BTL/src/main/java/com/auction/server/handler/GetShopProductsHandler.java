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
        System.out.println("[GetShopProductsHandler] Client " + conn.getRemoteSocketAddress() + " đang lấy danh sách shop...");

        try {
            // 1. Lấy sellerId từ request data
            String sellerId = (String) data.get("sellerId");

            // 2. Lấy filter status nếu có
            String statusFilter = (String) data.get("status");

            // 3. Lấy dữ liệu từ Database
            // ProductDao.getProductsByUserId sẽ lấy danh sách, trong đó image_path đã là link Cloudinary
            List<Product> rawList = ProductDao.getInstance().getProductsByUserId(sellerId);
            List<Product> result;

            if (statusFilter != null && !statusFilter.isEmpty()) {
                result = rawList.stream()
                        .filter(p -> p.getStatus().name().equalsIgnoreCase(statusFilter))
                        .collect(Collectors.toList());
            } else {
                result = rawList;
            }

            // --- BƯỚC NÀY ĐÃ ĐƯỢC "KHAI TỬ" ---
            // ĐÉO CẦN loop để readImageAsBase64 nữa.
            // Server bây giờ chỉ gửi cái link URL (imagePath) về cho Client thôi.
            // ----------------------------------------------

            // 4. Đóng gói response
            Response response = new Response(
                    MessageType.GET_SHOP_PRODUCTS_RESPONSE,
                    "SUCCESS",
                    "Lấy danh sách sản phẩm shop thành công"
            );
            response.getData().put("products", result);

            // Gửi phát một, nhẹ tênh vì không có chuỗi Base64 nặng nề
            conn.send(gson.toJson(response));

            System.out.println("[GetShopProductsHandler] Đã gửi " + result.size() + " sản phẩm. Client sẽ tự load ảnh từ URL.");

        } catch (Exception e) {
            System.err.println("[GetShopProductsHandler] Lỗi: " + e.getMessage());
            e.printStackTrace();
            conn.send(gson.toJson(new Response(MessageType.GET_SHOP_PRODUCTS_RESPONSE, "ERROR", e.getMessage())));
        }
    }
}