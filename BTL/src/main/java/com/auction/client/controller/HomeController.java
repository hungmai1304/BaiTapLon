package com.auction.client.controller;

import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.MessageListener;
import com.auction.client.network.NetworkClient;
import com.auction.client.network.RequestSender; // Import thêm class này để gửi dữ liệu mạng
import com.auction.client.utils.NavigationService;
import javafx.event.ActionEvent;
import com.auction.common.model.product.ProductStatus;
import com.auction.common.model.user.User; // Đảm bảo import Model User
import com.auction.protocol.MessageType;
import com.auction.protocol.Request;
import com.google.gson.Gson;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ToggleButton; // Import thêm ToggleButton cho fx:id nút quản trị
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HomeController implements Initializable {

    @FXML
    private BorderPane borderpane_home;

    @FXML
    private ToggleButton backToAdmin; // Khai báo fx:id ánh xạ chuẩn xác từ file FXML của bạn

    public BorderPane getBorderpaneHome() {
        return borderpane_home;
    }

    private void loadMainView() throws IOException  {
        StackPane main = FXMLLoader.load(
                getClass().getResource("/com/auction/client/view/main.fxml")
        );
        borderpane_home.setCenter(main);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        try {
            SomeGlobal.setHomeController(this);
            loadMainView();
            NavigationService.setTopView("/com/auction/client/view/topView.fxml");

            // --- Xử lý ẩn/hiện nút Quay lại Admin dựa trên Vai trò người dùng ---
            User currentUser = SomeGlobal.getCurrentUser();
            if (currentUser != null) {
                String role = currentUser.getRole(); // Giả định Model User của bạn có hàm getRole() trả về String

                if ("BIDDER".equalsIgnoreCase(role) || "SELLER".equalsIgnoreCase(role)) {
                    // Ẩn nút hoàn toàn khỏi giao diện và không chiếm không gian hiển thị
                    backToAdmin.setVisible(false);
                    backToAdmin.setManaged(false);
                } else if ("ADMIN".equalsIgnoreCase(role)) {
                    // Nếu là Admin thì hiển thị bình thường
                    backToAdmin.setVisible(true);
                    backToAdmin.setManaged(true);
                }
            } else {
                // Đề phòng trường hợp chưa có thông tin user, mặc định tạm ẩn
                backToAdmin.setVisible(false);
                backToAdmin.setManaged(false);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleHomeClicked(ActionEvent event) throws IOException {
        loadMainView();
    }

    @FXML
    public void handleSearchClicked(ActionEvent event) throws IOException {
        VBox search = FXMLLoader.load(getClass().getResource("/com/auction/client/view/search.fxml"));
        borderpane_home.setCenter(search);
    }

    @FXML
    public void handleTikTokAuction(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/tiktokAuction.fxml"));
            VBox tiktokView = loader.load();
            borderpane_home.setCenter(tiktokView);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleBankButtonClicked(ActionEvent event) throws IOException {
        StackPane bankView = FXMLLoader.load(getClass().getResource("/com/auction/client/view/bank.fxml"));
        borderpane_home.setCenter(bankView);
    }

    @FXML
    public void handleSettingClicked(ActionEvent event) throws IOException {
        AnchorPane settingView = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Settings.fxml"));
        borderpane_home.setCenter(settingView);
    }

    /**
     * Xử lý khi Admin nhấn nút quay lại màn hình Admin chuyên dụng
     */
    @FXML
    public void handleBackToAdmin(ActionEvent event) {
        User currentUser = SomeGlobal.getCurrentUser();

        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            System.out.println("[HomeController] Admin nhấn quay lại màn hình Admin. Đang gửi lệnh...");

            // Gửi tín hiệu lệnh "BACK_TO_ADMIN_COMMAND" thông qua RequestSender lên Server
            // Bạn có thể gửi kèm email của admin làm dữ liệu nhận biết
            RequestSender.send("BACK_TO_ADMIN_COMMAND", currentUser.getEmail());

            // Tùy chọn: Nếu bạn muốn chuyển đổi view ngay lập tức ở phía Client sang view AdminMain:
            // if (SomeGlobal.getAdminMainController() != null) { ... }
        }
    }
}