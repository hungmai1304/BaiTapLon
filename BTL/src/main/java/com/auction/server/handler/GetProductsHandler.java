package com.auction.server.handler;

import com.auction.common.model.product.Product; // Import class Product của nhóm anh
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@CommandMap(value = MessageType.GET_PRODUCTS_REQUEST)
public class GetProductsHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        System.out.println("[GetProductsHandler] Client " + conn.getRemoteSocketAddress() + " đang xin danh sách sản phẩm...");

        try {
            // 1. TẠO HÀNG GIẢ (Mai nối Database thì thay bằng: List<Product> list = ProductDAO.getAll(); )
            List<Product> productList = new ArrayList<>();

            Product p1 = new Product();
            p1.setId("P001");
            p1.setName("Laptop Gaming Asus ROG");
            p1.setCategory("Đồ Điện Tử");
            p1.setStartPrice(20000000);    // Giá khởi điểm
            p1.setCurrentPrice(25000000);  // Giá hiện tại đang được đấu
            p1.setStepPrice(500000);       // Bước giá mỗi lần bấm
            // Tạm thời chưa set owner và time để tránh lỗi Gson nhé

            Product p2 = new Product();
            p2.setId("P002");
            p2.setName("Mô hình Iron Man 1:1");
            p2.setCategory("Đồ Sưu Tầm");
            p2.setStartPrice(5000000);
            p2.setCurrentPrice(5000000);   // Chưa ai trả giá nên bằng giá khởi điểm
            p2.setStepPrice(100000);

            productList.add(p1);
            productList.add(p2);

            // 2. Đóng gói danh sách thành chuỗi JSON
            String productJsonData = gson.toJson(productList);

            // 3. Gửi về cho Client
            Response response = new Response(
                    MessageType.GET_PRODUCTS_RESPONSE,
                    "SUCCESS",
                    productJsonData // Nhét nguyên cái danh sách vào phần message/data
            );

            conn.send(gson.toJson(response));
            System.out.println("[GetProductsHandler] Đã gửi danh sách " + productList.size() + " món hàng thành công!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}