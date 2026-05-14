package com.auction.server.handler;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.auction.server.service.FileService; // Thêm import này
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandMap(value = MessageType.GET_SHOP_PRODUCTS_REQUEST)
public class GetShopProductsHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[GetShopProductsHandler] Client " + conn.getRemoteSocketAddress() + " hỏi sản phẩm shop...");

        try {
            // 1. Lấy sellerId từ request data
            String sellerId = (String) data.get("sellerId");

            // 2. Lấy filter status nếu có
            String statusFilter = (String) data.get("status");

            // 3. Lấy từ Database
            List<Product> rawList = ProductDao.getInstance().getProductsByUserId(sellerId);
            List<Product> result;

            if (statusFilter != null && !statusFilter.isEmpty()) {
                result = rawList.stream()
                        .filter(p -> p.getStatus().name().equalsIgnoreCase(statusFilter))
                        .collect(Collectors.toList());
            } else {
                result = rawList;
            }

            // --- BƯỚC QUAN TRỌNG: ĐỌC ẢNH TỪ FILE VẬT LÝ ---
            for (Product p : result) {
                if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
                    String base64 = FileService.readImageAsBase64(p.getImagePath());
                    p.setImageBase64(base64); // Nhét chuỗi ảnh vào để gửi đi
                }
            }
            // ----------------------------------------------

            // 4. Đóng gói response
            Response response = new Response(
                    MessageType.GET_SHOP_PRODUCTS_RESPONSE,
                    "SUCCESS",
                    "Lấy danh sách sản phẩm shop thành công"
            );
            response.getData().put("products", result);

            conn.send(gson.toJson(response));

            // Xoá Base64 trên Object sau khi gửi để tránh tốn RAM server (vì Product này có thể nằm trong context)
            for (Product p : result) {
                p.setImageBase64(null);
            }

            System.out.println("[GetShopProductsHandler] Đã gửi " + result.size() + " sản phẩm kèm dữ liệu ảnh!");

        } catch (Exception e) {
            System.err.println("[GetShopProductsHandler] Lỗi: " + e.getMessage());
            e.printStackTrace();
            // Nên gửi một response lỗi về để client không bị treo loading
            conn.send(gson.toJson(new Response(MessageType.GET_SHOP_PRODUCTS_RESPONSE, "ERROR", e.getMessage())));
        }
    }
}