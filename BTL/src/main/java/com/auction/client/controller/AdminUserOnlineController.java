package com.auction.client.controller;

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
import java.util.ResourceBundle;

public class AdminUserOnlineController implements Initializable {

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

        // Cấu hình CSS chỉ màu xanh lá cây cho sinh động bằng CellFactory:
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

        System.out.println("[AdminUserOnlineController] Đã map chuẩn xác các fx:id từ file FXML của bạn.");
    }

    /**
     * Hàm cấu hình sinh các nút chức năng đăng xuất từ xa trên từng dòng của cột Thao tác
     */
    /**
     * Hàm cấu hình sinh các nút chức năng (Đăng xuất & Ban) trên từng dòng của cột Thao tác
     */
    private void setupActionColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button btnKick = new Button("Đăng xuất");
                    private final Button btnBan = new Button("Ban");
                    private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8); // Khoảng cách giữa 2 nút là 8px

                    {
                        // 1. Định dạng style cho nút Đăng xuất (Kick)
                        btnKick.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnKick.setOnAction(event -> {
                            User selectedUser = getTableView().getItems().get(getIndex());
                            if (selectedUser != null && selectedUser.getEmail() != null) {
                                String email = selectedUser.getEmail();
                                System.out.println("[AdminUserOnlineController] Admin yêu cầu đăng xuất tài khoản: " + email);
                                RequestSender.send("ADMIN_LOGOUT_COMMAND", email);
                            }
                        });

                        // 2. Định dạng style cho nút Ban (Khóa)
                        // Sử dụng màu xám đậm hoặc đen để phân biệt với nút Đăng xuất đỏ
                        btnBan.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnBan.setOnAction(event -> {
                            User selectedUser = getTableView().getItems().get(getIndex());
                            if (selectedUser != null && selectedUser.getEmail() != null) {
                                String email = selectedUser.getEmail();
                                System.out.println("[AdminUserOnlineController] Admin yêu cầu BAN tài khoản: " + email);

                                // Gửi lệnh BAN lên Server qua mạng (Cần đồng bộ chuỗi lệnh này với Server Handler của bạn)
                                RequestSender.send("ADMIN_BAN_COMMAND", email);
                            }
                        });

                        // 3. Đưa các nút vào HBox container và căn giữa
                        container.setAlignment(javafx.geometry.Pos.CENTER);
                        container.getChildren().addAll(btnKick, btnBan);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(container); // Đưa container chứa cả 2 nút vào ô giao diện
                        }
                    }
                };
            }
        };

        colAction.setCellFactory(cellFactory);
    }
}