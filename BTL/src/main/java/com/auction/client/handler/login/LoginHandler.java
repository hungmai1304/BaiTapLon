package com.auction.client.handler.login;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.controller.login.LoginController;
import com.auction.client.controller.mainHome.BankController; // Import thêm BankController
import com.auction.client.network.IClientHandler;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;
import com.auction.common.model.user.User;
import com.auction.client.controller.general.SomeGlobal;
import javafx.application.Platform;

import static com.auction.client.utils.NavigationService.navigate;

@ResponseHandler(type = "LOGIN_RESPONSE")
public class LoginHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        LoginController controller = ControllerRegistry.get("LoginController");
        if (response == null) return;

        if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
            if (response.getData() != null) {
                // VIẾT THÊM: Đọc trạng thái tài khoản từ Server gửi về trước để kiểm tra ban/lock
                String status = (String) response.getData().get("status");

                // Nếu tài khoản bị BANNED thì chặn ngay lập tức, hiển thị thông báo lỗi và không cho vào App
                if ("BANNED".equalsIgnoreCase(status)) {
                    if (controller != null) {
                        Platform.runLater(() -> controller.updateAnnouncement("Tài khoản của bạn đã bị Admin khóa (BANNED)!"));
                    }
                    return;
                }

                String role = (String) response.getData().get("role");
                User user;
                if ("SELLER".equalsIgnoreCase(role)) {
                    user = new com.auction.common.model.user.Seller();
                    if (response.getData().containsKey("shopName")) {
                        ((com.auction.common.model.user.Seller) user).setShopName((String) response.getData().get("shopName"));
                    }
                } else {
                    user = new com.auction.common.model.user.Bidder();
                }
                user.setEmail((String) response.getData().get("email"));
                user.setUsername((String) response.getData().get("name"));
                user.setId((String) response.getData().get("id"));

                // VIẾT THÊM: Gán trạng thái vào đối tượng User cục bộ của Client
                user.setStatus(status != null ? status : "NORMAL");

                // gui yeu cau lay so du tai khoan user
                RequestSender.send(MessageType.GET_BALANCE_REQUEST, null);
                // ĐỌC THÊM GIÁ TRỊ BALANCE TỪ SERVER TRẢ VỀ KHI ĐĂNG NHẬP THÀNH CÔNG
                if (response.getData().containsKey("balance")) {
                    double balance = ((Number) response.getData().get("balance")).doubleValue();
                    user.setBalance(balance);
                }

                SomeGlobal.setCurrentUser(user);

                // Thực hiện chuyển màn hình Home
                navigate("/com/auction/client/view/home.fxml", "Auction - Trang chủ", true);

                // Tự động đồng bộ số dư sang màn hình Bank nếu view đã khởi tạo từ trước
                BankController bankCtrl = SomeGlobal.getBankController();
                if (bankCtrl != null) {
                    bankCtrl.updateBalanceOnUI(user.getBalance());
                }

                RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);
                ControllerRegistry.unregister("LoginController");
            } else {
                if (controller != null) {
                    String errorMsg = (response.getMessage() != null && !response.getMessage().isBlank())
                            ? response.getMessage()
                            : "Đăng nhập thất bại không nguyên do. Vui lòng kiểm tra lại!";
                    controller.updateAnnouncement(errorMsg);
                }
            }
        } else {
            if (controller != null) {
                controller.updateAnnouncement(response.getMessage());
            }
        }
    }
}