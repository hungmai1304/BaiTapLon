package com.auction.client.handler.shop;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.shop.ShopImportController;
import com.auction.client.network.IClientHandler;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry; // Dùng Registry cho đồng bộ với các file khác
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import javafx.application.Platform;

@ResponseHandler(type = "IMPORT_PRODUCT_RESPONSE")
public class ImportProductResponseHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        if (response == null) return;

        String status = response.getStatus();
        String message = response.getMessage();

        // Check Success ở đây
        boolean isSuccess = "SUCCESS".equalsIgnoreCase(status);

        // Lấy controller từ Registry (hoặc dùng getInstance() nếu mày chắc chắn nó chạy)
        ShopImportController controller = (ShopImportController) ControllerRegistry.get("ShopImportController");

        if (controller != null) {
            Platform.runLater(() -> {
                if (isSuccess) {
                    // Logic khi thành công (ví dụ: hiện chữ xanh, xóa form)
                    System.out.println("[importproductresponsehandler]Import thành công: " + message);
                    controller.updateSuccessLabel(message, true);
                    RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);

                    // Mày có thể gọi thêm hàm xóa trắng các ô nhập liệu ở đây nếu cần
                    // controller.clearFields();
                } else {
                    // Logic khi thất bại (ví dụ: hiện chữ đỏ)
                    System.err.println("[importproductresponsehandler] Import thất bại: " + message);
                    controller.updateSuccessLabel("[importproductresponsehandler]Lỗi: " + message, false);
                }
            });
        }
    }
}