package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.NavigationService;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import javafx.application.Platform;

@ResponseHandler(type = MessageType.EDIT_PRODUCT_RESPONSE)
public class EditProductClientHandler implements IClientHandler {
    @Override
    public void handle(Response response) {
        if ("SUCCESS".equals(response.getStatus())) {
            Platform.runLater(() -> {
                try {
                    // Cập nhật thành công thì tự động nhảy về trang My Shop
                    NavigationService.setCenterView("/com/auction/client/view/ShopSell.fxml");
                    // Xin lại danh sách để làm mới giao diện
                    RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }
}