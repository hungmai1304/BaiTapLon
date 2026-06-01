package com.auction.client.controller.admin;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.RequestSender;
import com.auction.protocol.MessageType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Logger;

import static com.auction.client.utils.NavigationService.navigate;

public class AdminMainController implements Initializable {
private static final Logger LOGGER = Logger.getLogger(AdminMainController.class.getName());
    @FXML
    private BorderPane mainBorderPane; // Khớp hoàn toàn với fx:id="mainBorderPane"

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Giả định đối tượng quản lý toàn cục lớp của bạn
        SomeGlobal.setAdminMainController(this);
        Platform.runLater(() -> {
            loadCenterView("/com/auction/client/view/adminOnlineUser.fxml");
        });
    }

    /**
     * Sự kiện khi nhấn vào nút "onlineUsers"
     */
    @FXML
    private void handleOnlineUsersClick(ActionEvent event) {
        loadCenterView("/com/auction/client/view/adminOnlineUser.fxml");
    }

    /**
     * Sự kiện khi nhấn vào nút "onlineAuctions"
     */
    @FXML
    private void handleOnlineAuctionsClick(ActionEvent event) {
        LOGGER.info("Chuyển sang màn hình quản lý đấu giá trực tuyến");
        loadCenterView("/com/auction/client/view/adminOnlineAuctions.fxml");
        LOGGER.info("(AdminMainController) dang gui ADMIN_GET_ONLINE_AUCTIONS_REQUEST");
        RequestSender.send("ADMIN_GET_ONLINE_AUCTIONS",null);


    }

    /**
     * Sự kiện khi nhấn vào nút "AllShop"
     */
    @FXML
    private void handleAllShopClick(ActionEvent event) {
        LOGGER.info("Chuyển sang màn hình quản lý toàn bộ cửa hàng");
        loadCenterView("/com/auction/client/view/adminAllShop.fxml");
        LOGGER.info("(AdminMainController) dang gui ADMIN_GET_ALL_SHOP");
        RequestSender.send("ADMIN_GET_ALL_SHOP",null);
    }

    /**
     * Sự kiện khi nhấn vào nút "banned list"
     */
    /**
     * Sự kiện khi nhấn vào nút "banned list"
     */
    @FXML
    private void handleBannedListClick(ActionEvent event) {
        LOGGER.info("Chuyển sang màn hình quản lý danh sách tài khoản bị cấm");

        // 1. Tải giao diện bảng Banned List lên vùng hiển thị trung tâm (Center của BorderPane)
        loadCenterView("/com/auction/client/view/bannedList.fxml");

        // 2. Gửi gói tin lệnh lên Server yêu cầu trả về danh sách User bị BANNED
        LOGGER.info("[Client] Đang gửi yêu cầu ADMIN_GET_BANNED_LIST lên server qua WebSocket...");

        // Truyền String lệnh thuần túy khớp với @CommandMap phía Server, tham số data truyền null vì không cần điều kiện lọc
        RequestSender.send("ADMIN_GET_BANNED_LIST", null);
    }

    /**
     * Sự kiện khi nhấn vào nút duyệt yêu cầu cấp quyền Admin ("More Admin")
     */
    @FXML
    private void handleMoreAdmin(ActionEvent event) {
        LOGGER.info("Chuyển sang màn hình duyệt danh sách xin làm Admin");

        // 1. Tải giao diện bảng duyệt lên vùng hiển thị trung tâm trước
        loadCenterView("/com/auction/client/view/acceptAdmin.fxml");

        // 2. Gửi gói tin request "GET_ADMIN_REQUEST_LIST" lên Server qua WebSocket
        LOGGER.info("[Client] Đang gửi yêu cầu GET_ADMIN_REQUEST_LIST lên server qua WebSocket...");
        RequestSender.send(MessageType.GET_ADMIN_REQUEST_LIST, null);
    }

    /**
     * Hàm dùng chung để load và đẩy view con vào khu vực Center của BorderPane
     */
    public void loadCenterView(String fxmlPath) {
        if (mainBorderPane == null) {
            LOGGER.severe("[LỖI] mainBorderPane vẫn bị null. Kiểm tra lại file FXML!");
            return;
        }

        try {
            URL fxmlUrl = getClass().getResource(fxmlPath);
            if (fxmlUrl == null) {
                LOGGER.severe("[LỖI] Không tìm thấy tập tin FXML tại đường dẫn: " + fxmlPath);
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node node = loader.load();

            // Thay đổi giao diện vùng Center thành công
            mainBorderPane.setCenter(node);
            LOGGER.info("[AdminMainController] Thay đổi vùng Center thành công: " + fxmlPath);

        } catch (IOException e) {
            LOGGER.severe("[LỖI] Gặp sự cố khi nạp giao diện con: " + fxmlPath);
            e.printStackTrace();
        }
    }

    /**
     * Sự kiện khi nhấn vào nút đăng xuất, chuyển hướng về màn hình Login và hủy session trên Server
     */
    @FXML
    public void handleLogout(ActionEvent event) throws IOException {
        // Tải file giao diện đăng nhập
        Parent loader = FXMLLoader.load(getClass().getResource("/com/auction/client/view/login.fxml"));
        Scene scene_login = new Scene(loader);

        // Lấy cửa sổ gốc hiện tại thông qua event nguồn bấm nút
        Stage prStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        // Tắt chế độ phóng to toàn màn hình nếu cần thiết, đặt lại scene đăng nhập và hiển thị
        prStage.setMaximized(false);
        prStage.setScene(scene_login);
        prStage.show();

        // Đồng thời gửi yêu cầu đăng xuất lên Server xóa Session kết nối
        LOGGER.info("[AdminMainController] Đang gửi yêu cầu đăng xuất LOGOUT_REQUEST lên server...");
        RequestSender.send(MessageType.LOGOUT_REQUEST, null);
    }
    @FXML
    public void handleSwitchToNormalInterface(ActionEvent event){
        // load giao dien main
        navigate("/com/auction/client/view/home.fxml", "Auction - Trang chủ", true);
    }
}