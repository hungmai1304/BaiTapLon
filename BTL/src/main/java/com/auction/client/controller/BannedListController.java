package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.AdminContext; // Sử dụng nếu bạn lưu danh sách trong AdminContext giống file mẫu
import com.auction.common.model.user.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.util.ResourceBundle;

public class BannedListController implements Initializable {

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
        // Đăng ký bộ điều khiển vào Registry / Global theo đúng phong cách file mẫu
        SomeGlobal.setBannedListController(this);

        // 1. Ánh xạ các cột cơ bản dựa vào Model User
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // 2. Logic tạo Số Thứ Tự (STT) tự động dựa trên vị trí hàng trong bảng (Copy từ file mẫu)
        colNo.setCellValueFactory(cellData -> {
            int index = userTable.getItems().indexOf(cellData.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        // 3. Logic cột Trạng thái (Hiển thị trạng thái dựa trên dữ liệu role/status của người dùng)
        colStatus.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            // Nếu xuất hiện trong file này thì trạng thái bao gồm: blacklist hoặc banned
            String status = (user != null && user.getRole() != null) ? user.getRole().toLowerCase() : "banned";
            return new SimpleStringProperty(status);
        });

        // Cấu hình CSS chữ màu đỏ/cam cho sinh động bằng CellFactory giống file mẫu:
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toUpperCase());
                    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Chữ màu đỏ báo hiệu tài khoản bị phạt
                }
            }
        });

        // 4. Logic tạo nút bấm cho cột Thao tác (colAction)
        setupActionColumn();

        // 5. Liên kết dữ liệu trực tiếp (Nếu dự án của bạn có danh sách trong AdminContext thì dùng dòng dưới,
        // hoặc bạn có thể tự thay thế bằng danh sách dữ liệu riêng của trang này nhé)
        // userTable.setItems(AdminContext.getInstance().getBannedUsers());

        System.out.println("[BannedListController] Đã map chuẩn xác các fx:id từ file FXML theo đúng file mẫu.");
    }

    /**
     * Hàm cấu hình sinh các nút chức năng (Bỏ cấm & Khôi phục Normal) trên từng dòng của cột Thao tác
     */
    private void setupActionColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button btnUnban = new Button("Bỏ cấm");
                    private final Button btnRestore = new Button("Khôi phục Normal");
                    private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(8); // Khoảng cách giữa 2 nút là 8px

                    {
                        // 1. Định dạng style cho nút Bỏ cấm (Màu cam nổi bật)
                        btnUnban.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnUnban.setOnAction(event -> {
                            User selectedUser = getTableView().getItems().get(getIndex());
                            if (selectedUser != null && selectedUser.getEmail() != null) {
                                String email = selectedUser.getEmail();
                                System.out.println("[BannedListController] Admin yêu cầu BỎ CẤM tài khoản: " + email);

                                // Gửi chuỗi lệnh String thuần túy lên Server giống hệt file mẫu
                                RequestSender.send("ADMIN_UNBAN_COMMAND", email);
                            }
                        });

                        // 2. Định dạng style cho nút Khôi phục Normal (Màu xanh lá)
                        btnRestore.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        btnRestore.setOnAction(event -> {
                            User selectedUser = getTableView().getItems().get(getIndex());
                            if (selectedUser != null && selectedUser.getEmail() != null) {
                                String email = selectedUser.getEmail();
                                System.out.println("[BannedListController] Admin yêu cầu KHÔI PHỤC NORMAL tài khoản: " + email);

                                // Gửi chuỗi lệnh String thuần túy lên Server giống hệt file mẫu
                                RequestSender.send("ADMIN_RESTORE_NORMAL_COMMAND", email);
                            }
                        });

                        // 3. Đưa các nút vào HBox container và căn giữa
                        container.setAlignment(javafx.geometry.Pos.CENTER);
                        container.getChildren().addAll(btnUnban, btnRestore);
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