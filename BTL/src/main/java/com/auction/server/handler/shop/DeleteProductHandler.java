package com.auction.server.handler.shop;

import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.dao.ProductDao;
import com.auction.server.handler.IMessageHandler;
import com.auction.server.model.ServerContext;
import com.google.gson.*;

import org.java_websocket.WebSocket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@CommandMap(value = MessageType.DELETE_PRODUCT_REQUEST)
public class DeleteProductHandler implements IMessageHandler {
    private static final Logger LOGGER = Logger.getLogger(DeleteProductHandler.class.getName());

    private static final Gson safeGson = new GsonBuilder()
            .registerTypeAdapter(LocalDateTime.class, (JsonSerializer<LocalDateTime>) (src, typeOfSrc, context) ->
                    new JsonPrimitive(src.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)))
            .registerTypeAdapter(LocalDateTime.class, (JsonDeserializer<LocalDateTime>) (json, typeOfT, context) ->
                    LocalDateTime.parse(json.getAsString(), DateTimeFormatter.ISO_LOCAL_DATE_TIME))
            .create();

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson defaultGson, ServerContext context) {
        try {
            // 1. Kiểm tra đăng nhập
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

            // KIỂM TRA QUYỀN SỞ HỮU TRƯỚC KHI XÓA
            // Lấy chính xác danh sách sản phẩm hợp pháp thuộc về Email này từ DB qua lệnh JOIN SQL
            List<Product> myProducts = ProductDao.getInstance().getProductsByUserEmail(userEmail);

            // Quét xem cái ID muốn xóa có nằm trong danh sách sở hữu của ông này không
            boolean isMyProduct = myProducts.stream().anyMatch(p -> p.getId().equals(productId));

            if (!isMyProduct) {
                conn.send(safeGson.toJson(new Response(MessageType.DELETE_PRODUCT_RESPONSE, "ERROR", "Bạn không có quyền xóa sản phẩm này!")));
                LOGGER.info(" [CẢNH BÁO BẢO MẬT] User " + userEmail + " cố tình xóa trái phép sản phẩm ID: " + productId);
                return;
            }
            // =================================================================

            // 3. Vượt qua vòng bảo mật -> Tiến hành xóa thẳng dưới DB
            boolean isDeleted = ProductDao.getInstance().deleteProduct(productId);

            if (isDeleted) {
                // 4. Lấy lại danh sách mới sạch sẽ sau khi xóa để Client vẽ lại giao diện
                List<Product> updatedList = ProductDao.getInstance().getProductsByUserEmail(userEmail);

                Response response = new Response(MessageType.DELETE_PRODUCT_RESPONSE, "SUCCESS", "Đã xóa sản phẩm!");
                response.getData().put("products", updatedList);

                conn.send(safeGson.toJson(response));
                LOGGER.info("[DeleteProduct] Đã xóa DB thành công ID: " + productId + " bởi " + userEmail);
            } else {
                conn.send(safeGson.toJson(new Response(MessageType.DELETE_PRODUCT_RESPONSE, "ERROR", "Không thể xóa trong Database!")));
            }
        } catch (Exception e) {
            e.printStackTrace();
            conn.send(safeGson.toJson(new Response(MessageType.DELETE_PRODUCT_RESPONSE, "ERROR", e.getMessage())));
        }
    }
}