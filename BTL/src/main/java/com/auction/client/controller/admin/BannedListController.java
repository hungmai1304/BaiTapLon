package com.auction.client.controller.admin;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.RequestSender;
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

public class BannedListController implements Initializable {

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> colNo;

    @FXML
    private TableColumn<User, String> colId;

    @FXML
    private TableColumn<User, String> colEmail;

    @FXML
    private TableColumn<User, String> colStatus;

    @FXML
    private TableColumn<User, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Đăng ký instance hiện tại ngay khi FXML vừa load xong
        SomeGlobal.setBannedListController(this);

        // Kiểm tra phân quyền an toàn ngay khi khởi tạo (Có thể dùng để ẩn/hiện bảng nếu cần)
        checkRolePermission();

        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // Tính số thứ tự (STT) tự động tăng theo dòng dữ liệu thực tế
        colNo.setCellValueFactory(cellData -> new SimpleStringProperty(""));
        colNo.setCellFactory(new Callback<TableColumn<User, String>, TableCell<User, String>>() {
            @Override
            public TableCell<User, String> call(TableColumn<User, String> param) {
                return new TableCell<User, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setText(null);
                        } else {
                            setText(String.valueOf(getIndex() + 1));
                        }
                    }
                };
            }
        });

        // Định dạng cột trạng thái hiển thị chữ BANNED màu đỏ nổi bật
        colStatus.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String status = (user != null && user.getRole() != null) ? user.getRole().toLowerCase() : "banned";
            return new SimpleStringProperty(status);
        });

        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item.toUpperCase());
                    setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-alignment: CENTER;");
                }
            }
        });

        // Khởi tạo cột chức năng duy nhất: Bỏ cấm (đã bao gồm check quyền)
        setupActionColumn();

        System.out.println("[UI] Khởi tạo BannedListController xong. Tự động gọi lệnh đòi danh sách từ Server...");

        // Chủ động gửi lệnh đòi danh sách bị cấm từ Server khi mở tab này
        RequestSender.send("ADMIN_GET_BANNED_LIST", new HashMap<>());
    }

    /**
     * Hàm kiểm tra phân quyền của người dùng hiện tại để đảm bảo an toàn hệ thống
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

    private void setupActionColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    private final Button btnUnban = new Button("Bỏ cấm");
                    private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox();

                    {
                        // 1. Định dạng style cho nút Bỏ cấm chuẩn chỉ theo form ông yêu cầu
                        btnUnban.setStyle("-fx-background-color: #e67e22; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");

                        btnUnban.setOnAction(event -> {
                            // Thực hiện check quyền Admin thực tế trước khi xử lý hành động
                            if (checkRolePermission()) {
                                User selectedUser = getTableView().getItems().get(getIndex());
                                if (selectedUser != null && selectedUser.getEmail() != null) {
                                    String email = selectedUser.getEmail();
                                    System.out.println("[BannedListController] Admin yêu cầu BỎ CẤM tài khoản: " + email);

                                    // Bọc email vào một đối tượng Map trước khi gửi để Server đồng bộ JSON tránh lỗi crash
                                    Map<String, Object> requestData = new HashMap<>();
                                    requestData.put("email", email);

                                    RequestSender.send("ADMIN_LET_USER_UNBAN", requestData);
                                }
                            } else {
                                System.out.println("[Từ chối hành động] Hệ thống ghi nhận bạn không có quyền Admin thực tế.");
                            }
                        });

                        // Căn giữa nút vào trong ô của TableView
                        container.setAlignment(javafx.geometry.Pos.CENTER);
                        container.getChildren().add(btnUnban);
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(container);
                        }
                    }
                };
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    public TableView<User> getUserTable() {
        return userTable;
    }
}