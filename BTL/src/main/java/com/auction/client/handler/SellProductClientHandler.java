package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.Response;
import javafx.application.Platform;

@ResponseHandler(type = "SELL_PRODUCT_RESPONSE")
public class SellProductClientHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        Platform.runLater(() -> {
            if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
                System.out.println("✅ [Client] Đã đưa sản phẩm lên sàn thành công!");
                System.out.println("   Message: " + response.getMessage());

                // TODO: Refresh lại UI - reload danh sách shop
                // Ví dụ: SomeGlobal.getShopController().refreshProductList();

            } else {
                System.err.println("❌ [Client] Lỗi khi lên sàn: " + response.getMessage());
            }
        });
    }
}