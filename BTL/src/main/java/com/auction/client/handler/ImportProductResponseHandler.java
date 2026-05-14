package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.ShopImportController;
import com.auction.client.network.IClientHandler; // Nhớ import cái này
import com.auction.protocol.Response;
import javafx.application.Platform;

@ResponseHandler(type = "IMPORT_PRODUCT_RESPONSE")
public class ImportProductResponseHandler implements IClientHandler { // BẮT BUỘC phải implements

    @Override // Ghi đè phương thức từ interface
    public void handle(Response response) {
        if (response == null) return;

        String status = response.getStatus();
        String message = response.getMessage();
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

        // Cập nhật UI
        ShopImportController controller = ShopImportController.getInstance();
        if (controller != null) {
            Platform.runLater(() -> {
                controller.updateSuccessLabel(message, isSuccess);
            });
        }
    }
}