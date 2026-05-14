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
import com.google.gson.reflect.TypeToken;
import javafx.application.Platform;

import java.util.List;

@ResponseHandler(type = MessageType.GET_AUCTION_PRODUCT_RESPONSE)
public class GetAuctionProductClientHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        try {
            // 1. Ép kiểu dữ liệu từ Map sang List<Product>
            // Lúc này field imagePath trong mỗi Product đã chứa link Cloudinary
            Object rawData = response.getData().get("products");
            List<Product> products = ClientMessageDispatcher.gson.fromJson(
                    ClientMessageDispatcher.gson.toJson(rawData),
                    new TypeToken<List<Product>>(){}.getType()
            );

            if (products != null && !products.isEmpty()) {
                // 2. Cập nhật vào Context (Để lưu trữ danh sách đang đấu giá)
                ClientContext.getInstance().setAuctionProducts(products);

                // 3. Ra lệnh cho UI hiển thị ngay lập tức
                Platform.runLater(() -> {
                    // Lấy controller từ Registry (đảm bảo mày đã register nó lúc initialize)
                    TikTokAuctionController controller = (TikTokAuctionController) ControllerRegistry.get("TikTokAuctionController");

                    if (controller != null) {
                        // Lấy sản phẩm hiện tại từ Context (thường là cái đầu tiên trong list)
                        Product current = ClientContext.getInstance().getCurrentProduct();

                        if (current != null) {
                            // Hàm updateUI này bên TikTokAuctionController sẽ dùng link URL để load ảnh
                            controller.updateUI(current);
                        }
                    }
                });

                System.out.println("-> [Handler] Nhận " + products.size() + " sản phẩm đấu giá từ Cloudinary.");
            } else {
                System.out.println("-> [Handler] Hiện tại không có sản phẩm nào đang đấu giá.");
            }

        } catch (Exception e) {
            System.err.println("-> [Handler Error] Lỗi khi xử lý danh sách đấu giá: " + e.getMessage());
            e.printStackTrace();
        }
    }
}