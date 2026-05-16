package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.BankController;
import com.auction.client.controller.MainController;
import com.auction.client.controller.SomeGlobal;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.protocol.Response;

@ResponseHandler(type = "GET_BALANCE_RESPONSE")
public class GetBalanceResponseHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        if (response == null) return;

        try {
            if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
                if (response.getData() != null && response.getData().containsKey("balance")) {
                    // 1. Trích xuất số dư (Current balance) từ Map data trong Response
                    double currentBalance = ((Number) response.getData().get("balance")).doubleValue();

                    // 2. Cập nhật số dư mới vào bộ nhớ Global của Client cho User hiện tại
                    if (SomeGlobal.getCurrentUser() != null) {
                        SomeGlobal.getCurrentUser().setBalance(currentBalance);
                    }
                    ClientContext.getInstance().updateBalance(currentBalance);

                    // 3. Đẩy số dư này trực tiếp lên giao diện Bank nếu giao diện đang mở
                    BankController bankCtrl = SomeGlobal.getBankController();
                    if (bankCtrl != null) {
                        bankCtrl.updateBalanceOnUI(currentBalance);
                    }


                    System.out.println("[GetBalanceResponseHandler] Cập nhật số dư thành công: " + currentBalance);
                }
            } else {
                System.err.println("[GetBalanceResponseHandler] Lỗi từ Server: " + response.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[GetBalanceResponseHandler] Lỗi xử lý gói tin số dư: " + e.getMessage());
        }
    }
}