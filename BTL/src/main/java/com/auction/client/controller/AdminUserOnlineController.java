package com.auction.client.controller;

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
        // Bạn có thể custom CSS chữ màu xanh lá cây cho sinh động bằng CellFactory nếu muốn:
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
     * Hàm cấu hình sinh các nút chức năng (ví dụ: nút Kick/Khóa) trên từng dòng của cột Thao tác
     */
    private void setupActionColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button btnKick = new Button("Đăng xuất");

                    {
                        // Định dạng style cơ bản cho nút bấm
                        btnKick.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand;");

                        // Xử lý sự kiện khi Admin bấm nút này trên một hàng bất kỳ
                        btnKick.setOnAction(event -> {
                            User selectedUser = getTableView().getItems().get(getIndex());
                            System.out.println("Admin yêu cầu xử lý người dùng: " + selectedUser.getEmail());

                            // Gửi request Kick/Khóa lên Server nếu hệ thống của bạn có tính năng này
                            // RequestSender.send(MessageType.ADMIN_KICK_USER_REQUEST, selectedUser.getId());
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(btnKick); // Đưa nút bấm vào ô giao diện
                        }
                    }
                };
            }
        };

        colAction.setCellFactory(cellFactory);
    }
}