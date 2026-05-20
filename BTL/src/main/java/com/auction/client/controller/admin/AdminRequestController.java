package com.auction.client.controller.admin;

// Thay thế đường dẫn Model User thực tế của bạn tại đây:
import com.auction.common.model.user.User;
        import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

public class AdminRequestController {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Integer> colNo;
    @FXML private TableColumn<User, String> colName;
    @FXML private TableColumn<User, String> colEmail;
    @FXML private TableColumn<User, String> colStatus; // Hoặc kiểu dữ liệu Enum trạng thái của bạn
    @FXML private TableColumn<User, Void> colAction;

    @FXML
    public void initialize() {
        // 1. Đổ dữ liệu từ Context vào Table (sửa lại hàm get cho đúng với App của bạn)
        // userTable.setItems(ClientContext.getInstance().getAdminUsers());

        // 2. Cấu hình cột Số Thứ Tự (STT) tự động tăng
        colNo.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        // 3. Cấu hình các cột hiển thị thông tin Text cơ bản
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // 4. Cấu hình cột trạng thái (Tùy biến màu sắc giống ShopSellController)
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                    return;
                }

                // Thí dụ custom màu sắc dựa theo chuỗi trạng thái nhận được
                setText(item);
                if (item.equalsIgnoreCase("PENDING")) {
                    setStyle("-fx-text-fill: #f0a500; -fx-font-weight: bold;"); // Màu cam chờ duyệt
                } else if (item.equalsIgnoreCase("APPROVED")) {
                    setStyle("-fx-text-fill: #4caf50; -fx-font-weight: bold;"); // Màu xanh chấp nhận
                } else {
                    setStyle("-fx-text-fill: #f44336; -fx-font-weight: bold;"); // Màu đỏ từ chối
                }
            }
        });

        // 5. Cấu hình cột Thao tác (Chứa 2 nút: Chấp nhận & Từ chối)
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnAccept = new Button("Chấp nhận");
            private final Button btnReject = new Button("Từ chối");
            private final HBox box = new HBox(10, btnAccept, btnReject);

            {
                box.setAlignment(javafx.geometry.Pos.CENTER);

                // Style nút Chấp nhận (Xanh lá) & Từ chối (Đỏ)
                btnAccept.setStyle("-fx-background-color: #4caf50; -fx-text-fill: white; -fx-cursor: hand;");
                btnReject.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-cursor: hand;");

                // Sự kiện khi click nút Chấp nhận
                btnAccept.setOnAction(e -> {
                    User selectedUser = getTableView().getItems().get(getIndex());
                    handleAccept(selectedUser);
                });

                // Sự kiện khi click nút Từ chối
                btnReject.setOnAction(e -> {
                    User selectedUser = getTableView().getItems().get(getIndex());
                    handleReject(selectedUser);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });
    }

    /**
     * Xử lý khi Admin nhấn nút Chấp nhận
     */
    private void handleAccept(User user) {
        System.out.println("Chấp nhận người dùng: " + user.getEmail());
        // Thêm hàm gửi Request lên server của bạn tại đây, ví dụ:
        // RequestSender.sendAcceptUserRequest(user.getId());
    }

    /**
     * Xử lý khi Admin nhấn nút Từ chối
     */
    private void handleReject(User user) {
        System.out.println("Từ chối người dùng: " + user.getEmail());
        // Thêm hàm gửi Request lên server của bạn tại đây, ví dụ:
        // RequestSender.sendRejectUserRequest(user.getId());
    }
}