package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.TikTokAuctionController;
import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.ControllerRegistry;
import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.util.List;

@ResponseHandler(type = MessageType.GET_AUCTION_PRODUCT_RESPONSE)
public class GetAuctionProductClientHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        try {
            // 1. Ép kiểu dữ liệu từ Map sang List<Product> bằng Gson
            Object rawData = response.getData().get("products");
            List<Product> products = ClientMessageDispatcher.gson.fromJson(
                    ClientMessageDispatcher.gson.toJson(rawData),
                    new TypeToken<List<Product>>(){}.getType()
            );

            if (products != null && !products.isEmpty()) {
                // 2. Cập nhật vào Context (Hàm setAuctionProducts bạn vừa viết)
                ClientContext.getInstance().setAuctionProducts(products);

                // 3. Quan trọng: Ra lệnh cho UI hiển thị ngay lập tức
                Platform.runLater(() -> {
                    TikTokAuctionController controller = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");
                    if (controller != null) {
                        // Lấy sản phẩm đầu tiên vừa nhận được để hiển thị
                        controller.updateUI(ClientContext.getInstance().getCurrentProduct());
                    }
                });

                System.out.println("✅ [Handler] Đã cập nhật " + products.size() + " sản phẩm lên màn hình.");
            }
        } catch (Exception e) {
            System.err.println("❌ [Handler Error] Lỗi khi xử lý danh sách sản phẩm: " + e.getMessage());
            e.printStackTrace();
        }
    }
}