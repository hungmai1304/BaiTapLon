package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.AdminContext;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.util.Callback;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminAcceptController implements Initializable {

    @FXML
    private TableView<User> userTable;

    @FXML
    private TableColumn<User, String> colNo;       // Cột Số thứ tự tự động tăng

    @FXML
    private TableColumn<User, String> colId;       // Cột ID người dùng

    @FXML
    private TableColumn<User, String> colEmail;    // Cột Email người dùng

    @FXML
    private TableColumn<User, String> colStatus;   // Cột Trạng thái (Hiển thị động)

    @FXML
    private TableColumn<User, Void> colAction;     // Cột chứa 2 nút: Duyệt (Allow) và Từ chối (Reject)

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Ánh xạ dữ liệu cơ bản từ đối tượng User
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));

        // 2. Tự động tính Số Thứ Tự (STT) dựa trên vị trí hàng
        colNo.setCellValueFactory(cellData -> {
            int index = userTable.getItems().indexOf(cellData.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        // 3. ĐỌC TRẠNG THÁI ĐỘNG: Lấy từ thuộc tính status của Object User (Mặc định là "Chờ duyệt")
        colStatus.setCellValueFactory(cellData -> {
            User u = cellData.getValue();
            // Lưu ý: Đảm bảo class User của bạn đã có thuộc tính status cùng getter/setter tương ứng.
            // Nếu u.getStatus() trả về null, ta coi như bản ghi đó đang ở trạng thái mặc định "Chờ duyệt"
            String status = (u.getRole() != null) ? u.getRole() : "ADMIN_REQUEST";
            return new SimpleStringProperty(status);
        });

        // Cấu hình hiển thị màu sắc tương ứng theo từng trạng thái cụ thể
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    // Đổi màu sắc font chữ tùy thuộc vào chuỗi trạng thái hiện tại
                    if ("Chờ duyệt".equals(item)) {
                        setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;"); // Màu vàng/cam
                    } else if ("Đã phê duyệt".equals(item)) {
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Màu xanh lá
                    } else if ("Đã từ chối".equals(item)) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;"); // Màu đỏ
                    }
                }
            }
        });

        // 4. Tạo bộ nút thao tác (Duyệt & Từ chối)
        setupActionColumn();

        // 5. Đổ dữ liệu từ AdminContext vào TableView công khai
        // Nhờ ObservableList, khi AdminRequestHandler nhận data mới từ Server, TableView sẽ tự động cập nhật tự quản
        userTable.setItems(AdminContext.getInstance().getAdminRequests());
    }

    /**
     * Hàm cấu hình tạo cùng lúc 2 nút chức năng (Allow & Reject) trên từng dòng
     */
    private void setupActionColumn() {
        Callback<TableColumn<User, Void>, TableCell<User, Void>> cellFactory = new Callback<>() {
            @Override
            public TableCell<User, Void> call(final TableColumn<User, Void> param) {
                return new TableCell<>() {
                    // Tạo một container HBox để chứa song song 2 nút bấm
                    private final HBox container = new HBox(10); // Khoảng cách giữa 2 nút là 10px
                    private final Button btnAllow = new Button("Duyệt");
                    private final Button btnReject = new Button("Từ chối");

                    {
                        // Cấu hình giao diện cho nút Duyệt (Màu xanh lá)
                        btnAllow.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");
                        // Cấu hình giao diện cho nút Từ chối (Màu đỏ)
                        btnReject.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold;");

                        // Cân giữa các nút bấm bên trong ô dữ liệu
                        container.setStyle("-fx-alignment: CENTER;");
                        container.getChildren().addAll(btnAllow, btnReject);

                        // --- XỬ LÝ SỰ KIỆN KHI BẤM NÚT DUYỆT ---
                        btnAllow.setOnAction(event -> {
                            User selectedUser = getTableView().getItems().get(getIndex());
                            System.out.println("[AdminAccept] Chấp nhận yêu cầu làm Admin của: " + selectedUser.getEmail());

                            // 1. Cập nhật trạng thái trực tiếp vào Object User dưới Client bộ nhớ tạm
                            selectedUser.setRole("ADMIN");

                            // 2. Ép TableView vẽ lại (refresh) giao diện để kích hoạt đổi chữ & màu
                            getTableView().refresh();

                            // 3. Đóng gói gói tin Map gửi ID yêu cầu lên Server xử lý bất đồng bộ
                            Map<String, Object> reqData = new HashMap<>();
                            reqData.put("userId", selectedUser.getId());

                            // Gửi đi bằng RequestSender của bạn
                            RequestSender.send(MessageType.ADMIN_ACCEPT_REQUEST, reqData);
                        });

                        // --- XỬ LÝ SỰ KIỆN KHI BẤM NÚT TỪ CHỐI ---
                        btnReject.setOnAction(event -> {
                            User selectedUser = getTableView().getItems().get(getIndex());
                            System.out.println("[AdminAccept] Từ chối yêu cầu làm Admin của: " + selectedUser.getEmail());

                            // 1. Cập nhật trạng thái trực tiếp vào Object User dưới Client bộ nhớ tạm
                            selectedUser.setRole("BIDDER");

                            // 2. Ép TableView vẽ lại (refresh) giao diện để kích hoạt đổi chữ & màu
                            getTableView().refresh();

                            // 3. Đóng gói gói tin Map gửi ID yêu cầu từ chối lên Server xử lý
                            Map<String, Object> reqData = new HashMap<>();
                            reqData.put("userId", selectedUser.getId());

                            // Gửi đi bằng RequestSender của bạn
                            RequestSender.send(MessageType.ADMIN_REJECT_REQUEST, reqData);
                        });
                    }

                    @Override
                    protected void updateItem(Void item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            setGraphic(container); // Đưa toàn bộ HBox chứa 2 nút vào ô giao diện
                        }
                    }
                };
            }
        };

        colAction.setCellFactory(cellFactory);
    }
}