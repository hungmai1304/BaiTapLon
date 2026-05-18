package com.auction.server.handler;

import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.model.ServerContext;
import com.google.gson.*;

import org.java_websocket.WebSocket;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@CommandMap(value = MessageType.DELETE_PRODUCT_REQUEST)
public class DeleteProductHandler implements IMessageHandler {

    // BỌC THÉP GSON ĐỂ KHÔNG BỊ LỖI THỜI GIAN KHI GỬI DANH SÁCH VỀ CLIENT
    private static final Gson safeGson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson defaultGson, ServerContext context) {
        try {
            // 1. Lấy Email người dùng để lấy đúng danh sách Shop của người đó
            String userEmail = context.getUserByConn(conn);
            if (userEmail == null) {
                conn.send(safeGson.toJson(new Response(MessageType.DELETE_PRODUCT_RESPONSE, "ERROR", "Bạn chưa đăng nhập!")));
                return;
            }

            // 2. Lấy ID sản phẩm cần xóa
            String productId = (String) data.get("id");
            if (productId == null || productId.isEmpty()) {
                conn.send(safeGson.toJson(new Response(MessageType.DELETE_PRODUCT_RESPONSE, "ERROR", "Thiếu ID sản phẩm!")));
                return;
            }

            // 3. XÓA THẲNG XUỐNG DATABASE
            boolean isDeleted = ProductDao.getInstance().deleteProduct(productId);

            if (isDeleted) {

                // 4. Lấy lại danh sách mới nhất từ DB
                List<Product> updatedList = ProductDao.getInstance().getProductsByUserEmail(userEmail);

                // 5. Đóng gói và gửi về cho Client
                Response response = new Response(MessageType.DELETE_PRODUCT_RESPONSE, "SUCCESS", "Đã xóa sản phẩm!");
                response.getData().put("products", updatedList);

                conn.send(safeGson.toJson(response));
                System.out.println("[DeleteProduct] Đã xóa DB thành công ID: " + productId);
            } else {
                conn.send(safeGson.toJson(new Response(MessageType.DELETE_PRODUCT_RESPONSE, "ERROR", "Không thể xóa trong Database!")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            conn.send(safeGson.toJson(new Response(MessageType.DELETE_PRODUCT_RESPONSE, "ERROR", e.getMessage())));
        }
    }
}