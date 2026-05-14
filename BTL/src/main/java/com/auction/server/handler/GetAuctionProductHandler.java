package com.auction.server.handler;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.server.annotation.CommandMap;
import com.auction.server.model.ServerContext;
import com.auction.server.service.FileService; // Thêm import này
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@CommandMap(value = MessageType.GET_AUCTION_PRODUCT_REQUEST)
public class GetAuctionProductHandler implements IMessageHandler {

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {
        try {
            System.out.println("[Handler] Đang xử lý lấy sản phẩm đang đấu giá...");
            List<Product> allProducts = context.getProductList();

            if (allProducts == null) {
                System.out.println("[Handler] Cảnh báo: allProducts bị null");
                allProducts = new ArrayList<>();
            }

            // 1. Lọc các sản phẩm đang có trạng thái ON_AUCTION
            List<Product> auctionProducts = allProducts.stream()
                    .filter(p -> p != null && p.getStatus() != null && p.getStatus() == ProductStatus.ON_AUCTION)
                    .collect(Collectors.toList());

            // 2. QUAN TRỌNG: Với mỗi sản phẩm, đọc file ảnh từ imagePath và chuyển thành Base64
            for (Product p : auctionProducts) {
                if (p.getImagePath() != null && !p.getImagePath().isEmpty()) {
                    // Đọc từ server_data/product_images/... thành chuỗi Base64
                    String base64 = FileService.readImageAsBase64(p.getImagePath());
                    p.setImageBase64(base64);
                }
            }

            System.out.println("[Handler] Tìm thấy " + auctionProducts.size() + " sản phẩm ON_AUCTION.");

            Response response = new Response(
                    MessageType.GET_AUCTION_PRODUCT_RESPONSE,
                    "SUCCESS",
                    "Lấy sản phẩm thành công"
            );

            response.getData().put("products", auctionProducts);

            // 3. Chuyển sang JSON và gửi đi
            String jsonResponse;
            try {
                jsonResponse = gson.toJson(response);
            } catch (Exception jsonEx) {
                System.err.println("[Handler] Lỗi GSON không thể chuyển Product sang JSON: " + jsonEx.getMessage());
                throw jsonEx;
            }

            conn.send(jsonResponse);

            // Sau khi gửi xong, ta nên set lại Base64 về null để giải phóng bộ nhớ RAM cho ServerContext
            // Nếu danh sách sản phẩm lớn, việc giữ Base64 trong RAM sẽ làm Server bị chậm
            for (Product p : auctionProducts) {
                p.setImageBase64(null);
            }

            System.out.println("[Handler] Đã gửi danh sách sản phẩm kèm ảnh thành công!");

        } catch (Exception e) {
            System.err.println("[Handler ERROR]: " + e.toString());
            e.printStackTrace();
            String errorMsg = e.getMessage() != null ? e.getMessage() : "Unknown Error";
            conn.send("{\"type\":\"" + MessageType.GET_AUCTION_PRODUCT_RESPONSE + "\", \"status\":\"ERROR\", \"message\":\"" + errorMsg + "\"}");
        }
    }
}