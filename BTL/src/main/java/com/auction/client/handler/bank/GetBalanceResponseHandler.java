package com.auction.client.handler.bank;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.mainHome.BankController;
import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.IClientHandler;
import com.auction.client.utils.ClientContext;
import com.auction.protocol.Response;

import java.util.logging.Logger;

@ResponseHandler(type = "GET_BALANCE_RESPONSE")
public class GetBalanceResponseHandler implements IClientHandler {
private static final Logger LOGGER = Logger.getLogger(GetBalanceResponseHandler.class.getName());
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


                    LOGGER.info("[GetBalanceResponseHandler] Cập nhật số dư thành công: " + currentBalance);
                }
            } else {
                LOGGER.severe("[GetBalanceResponseHandler] Lỗi từ Server: " + response.getMessage());
            }
        } catch (Exception e) {
            LOGGER.severe("[GetBalanceResponseHandler] Lỗi xử lý gói tin số dư: " + e.getMessage());
        }
    }
}