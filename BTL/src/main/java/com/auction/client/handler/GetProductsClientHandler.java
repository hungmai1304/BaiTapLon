package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.common.model.product.Product;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.lang.reflect.Type;
import java.util.List;

// Lắng nghe đúng nhãn GET_PRODUCTS_RESPONSE từ Server
@ResponseHandler(type = "GET_PRODUCTS_RESPONSE")
public class GetProductsClientHandler implements IClientHandler {

    private final Gson gson = new Gson();

    @Override
    public void handle(Response response) {
        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {

            // Đưa về luồng UI của JavaFX để không bị đơ màn hình
            Platform.runLater(() -> {
                System.out.println("✅ [GetProductsClientHandler] Đã nhận kho hàng về màn hình Home!");

                try {
                    // 💡 Điểm khác biệt: Dùng Gson ép kiểu thành 1 DANH SÁCH (List<Product>)
                    String jsonData = gson.toJson(response.getData());
                    Type listType = new TypeToken<List<Product>>(){}.getType();
                    List<Product> productList = gson.fromJson(jsonData, listType);

                    // In ra Terminal để test thử
                    System.out.println("📦 Tổng số mặt hàng nhận được: " + productList.size());
                    for (Product p : productList) {
                        System.out.println("- " + p.getName() + " | Giá: " + p.getCurrentPrice());
                    }

                    // 👉 BƯỚC TIẾP THEO (Sau khi log chạy ngon):
                    // Gọi HomeController ra để vẽ cái List này lên giao diện
                    // Ví dụ: SomeGlobal.getHomeController().hienThiDanhSachSanPham(productList);

                } catch (Exception e) {
                    System.err.println("❌ Lỗi phân tích danh sách sản phẩm: " + e.getMessage());
                }
            });

        } else {
            System.err.println("❌ Lỗi từ Server: " + response.getMessage());
        }
    }
}