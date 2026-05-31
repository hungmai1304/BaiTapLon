package com.auction.client.controller.admin;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.RequestSender;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.util.Callback;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.logging.Logger;

public class AdminOnlineAuctions implements Initializable {
private static final Logger LOGGER = Logger.getLogger(AdminOnlineAuctions.class.getName());
    @FXML
    private TableView<AuctionItem> auctionTable;
    @FXML
    private TableColumn<AuctionItem, String> colNo;
    @FXML
    private TableColumn<AuctionItem, String> colId;
    @FXML
    private TableColumn<AuctionItem, String> colProductName;
    @FXML
    private TableColumn<AuctionItem, String> colOwner;
    @FXML
    private TableColumn<AuctionItem, String> colStatus;
    @FXML
    private TableColumn<AuctionItem, Void> colAction;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        SomeGlobal.setAdminOnlineAuctionsController(this);

        // 1. Map dữ liệu
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colProductName.setCellValueFactory(new PropertyValueFactory<>("productName"));
        colOwner.setCellValueFactory(new PropertyValueFactory<>("ownerEmail"));

        // 2. Số thứ tự tự động
        colNo.setCellValueFactory(cellData -> {
            int index = auctionTable.getItems().indexOf(cellData.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        // 3. Đổi màu chữ theo trạng thái đấu giá (ADVERTISING / ONGOING)
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colStatus.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    if ("ADVERTISING".equalsIgnoreCase(item)) {
                        setText("Đang quảng cáo");
                        setStyle("-fx-text-fill: #e67e22; -fx-font-weight: bold;"); // Màu cam
                    } else if ("ONGOING".equalsIgnoreCase(item)) {
                        setText("Đang diễn ra");
                        setStyle("-fx-text-fill: #2ecc71; -fx-font-weight: bold;"); // Màu xanh lá
                    } else {
                        setText(item);
                        setStyle("-fx-text-fill: #7f8c8d;");
                    }
                }
            }
        });

        // 4. Sinh các nút chức năng trong cột Thao tác
        setupActionColumn();
    }

    private void setupActionColumn() {
        Callback<TableColumn<AuctionItem, Void>, TableCell<AuctionItem, Void>> cellFactory = param -> new TableCell<>() {
            private final Button btnStop = new Button("Hủy cuộc họp");
            private final javafx.scene.layout.HBox container = new javafx.scene.layout.HBox(btnStop);
            {
                btnStop.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 11px;");
                btnStop.setOnAction(event -> {
                    AuctionItem selected = getTableView().getItems().get(getIndex());
                    if (selected != null) {
                        // Gọi lệnh hủy phiên đấu giá lên server nếu admin yêu cầu can thiệp
                        Map<String, Object> req = new HashMap<>();
                        req.put("productId", selected.getId());
                        RequestSender.send("ADMIN_CANCEL_AUCTION", req);
                    }
                });
                container.setAlignment(javafx.geometry.Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        };
        colAction.setCellFactory(cellFactory);
    }

    public void updateTableData(List<AuctionItem> list) {
        auctionTable.setItems(FXCollections.observableArrayList(list));
        auctionTable.refresh();
    }

    // DTO bọc dữ liệu phiên đấu giá nhận từ JSON
    public static class AuctionItem {
        private String id;
        private String productName;
        private String status;
        private String ownerEmail;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getProductName() { return productName; }
        public void setProductName(String productName) { this.productName = productName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getOwnerEmail() { return ownerEmail; }
        public void setOwnerEmail(String ownerEmail) { this.ownerEmail = ownerEmail; }
    }
}