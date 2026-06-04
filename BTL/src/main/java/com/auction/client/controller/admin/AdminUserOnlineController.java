package com.auction.client.controller.admin;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.AdminContext;
import com.auction.common.model.user.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class AdminUserOnlineController implements Initializable {
private static final Logger LOGGER = Logger.getLogger(AdminUserOnlineController.class.getName());
    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> colNo; // Cột số thứ tự tự động tăng

    @FXML
    private TableColumn<User, String> colId;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colStatus; // Cột trạng thái

    @FXML
    private TableColumn<User, Void> colAction; // Cột chứa nút thao tác công cụ


    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đăng ký bộ điều khiển vào Registry / Global nếu cần thiết
        SomeGlobal.setAdminUserOnlineController(this);

        // Gọi hàm kiểm tra phân quyền người dùng
        checkRolePermission();

        // 1. Ánh xạ các cột cơ bản dựa vào Model User
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // 2. Logic tạo Số Thứ Tự (STT) tự động dựa trên vị trí hàng trong bảng
        colNo.setCellValueFactory(cellData -> {
            int index = userTable.getItems().indexOf(cellData.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        // 3. Logic cột Trạng thái (Vì danh sách này lấy từ AdminContext.onlineUsers nên mặc định là Online)
        colStatus.setCellValueFactory(cellData -> new SimpleStringProperty("Trực tuyến"));

        // Cấu hình CSS chữ màu xanh lá cây cho sinh động bằng CellFactory:
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Chữ màu xanh lá
                }
            }
        });

        // 4. Logic tạo nút bấm cho cột Thao tác (colAction)
        setupActionColumn();

        // 5. Liên kết dữ liệu trực tiếp với AdminContext
        userTable.setItems(AdminContext.getInstance().getOnlineUsers());

        LOGGER.info("[AdminUserOnlineController] Đã map chính xác các fx:id từ file FXML của bạn.");
    }

    /**
     * Hàm kiểm tra phân quyền của người dùng hiện tại để ẩn/hiển nút quay lại giao diện Admin
     */
    private boolean checkRolePermission() {
        User currentUser = SomeGlobal.getCurrentUser();
        String role = (currentUser != null && currentUser.getRole() != null) ? currentUser.getRole().trim() : "";

        if ("ADMIN".equalsIgnoreCase(role)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Hàm cấu hình sinh các nút chức năng (Đăng xuất, Ban & Blacklist) trên từng dòng của cột Thao tác
     */
    private void setupActionColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button btnKick = new Button("Đăng xuất");
                    private final Button btnBan = new Button("Ban");
                    private final Button btnBlacklist = new Button("Blacklist"); // THÊM MỚI: Nút Blacklist
                    private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8); // Khoảng cách giữa các nút là 8px

                    {
                        // 1. Định dạng style cho nút Đăng xuất (Kick)
                        btnKick.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnKick.setOnAction(event -> {
                            if(checkRolePermission()){
                                User selectedUser = getTableView().getItems().get(getIndex());
                                if (selectedUser != null && selectedUser.getEmail() != null) {
                                    String email = selectedUser.getEmail();
                                    Map<String, Object> requestData = new HashMap<>();
                                    requestData.put("email", email);
                                    LOGGER.info("[AdminUserOnlineController] Admin yêu cầu đăng xuất tài khoản: " + email);
                                    RequestSender.send("ADMIN_LET_USER_LOGOUT", requestData);
                                }
                            }
                            else {
                                LOGGER.info("[Từ chối hành động] Hệ thống ghi nhận bạn không có quyền Admin thực tế.");
                            }
                        });

                        // 2. Định dạng style cho nút Ban (Khóa)
                        btnBan.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnBan.setOnAction(event -> {
                            if(checkRolePermission()){
                                User selectedUser = getTableView().getItems().get(getIndex());
                                if (selectedUser != null && selectedUser.getEmail() != null) {
                                    String email = selectedUser.getEmail();
                                    LOGGER.info("[AdminUserOnlineController] Admin yêu cầu BAN tài khoản: " + email);

                                    Map<String, Object> requestData = new HashMap<>();
                                    requestData.put("email", email);

                                    RequestSender.send("ADMIN_LET_USER_BE_BANNED", requestData);
                                }
                            }
                            else {
                                LOGGER.info("[Từ chối hành động] Hệ thống ghi nhận bạn không có quyền Admin thực tế.");
                            }
                        });

                        // 3. THÊM MỚI: Định dạng style và xử lý sự kiện cho nút Blacklist
                        btnBlacklist.setStyle("-fx-background-color: #d35400; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;"); // Màu cam đậm quyến rũ
                        btnBlacklist.setOnAction(event -> {
                            if(checkRolePermission()){
                                User selectedUser = getTableView().getItems().get(getIndex());
                                if (selectedUser != null && selectedUser.getEmail() != null) {
                                    String email = selectedUser.getEmail();
                                    LOGGER.info("[AdminUserOnlineController] Admin yêu cầu BLACKLIST tài khoản: " + email);

                                    // Bọc email vào Map để đồng bộ cấu trúc JSON gửi lên Server
                                    Map<String, Object> requestData = new HashMap<>();
                                    requestData.put("email", email);

                                    // Gửi lệnh BLACKLIST lên Server
                                    RequestSender.send("ADMIN_LET_USER_BE_BLACKLIST", requestData);
                                }
                            }
                            else {
                                LOGGER.info("[Từ chối hành động] Hệ thống ghi nhận bạn không có quyền Admin thực tế.");
                            }
                        });

                        // 4. Đưa cả 3 nút vào HBox container và căn giữa
                        container.setAlignment(javafx.geometry.Pos.CENTER);
                        container.getChildren().addAll(btnKick, btnBan, btnBlacklist);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(container); // Đưa container chứa cả 3 nút vào ô giao diện
                        }
                    }
                };
            }
        };

        colAction.setCellFactory(cellFactory);
    }
}