package com.auction.client.controller.admin;

import com.auction.client.controller.general.SomeGlobal;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class AllShopController implements Initializable {

    @FXML
    private TableView<ShopItem> userTable; // Đổi kiểu dữ liệu sang ShopItem để nhận trường productCount

    @FXML
    private TableColumn<ShopItem, String> colNo;

    @FXML
    private TableColumn<ShopItem, String> colId;

    @FXML
    private TableColumn<ShopItem, String> colEmail;

    @FXML
    private TableColumn<ShopItem, String> colShopName;

    @FXML
    private TableColumn<ShopItem, Integer> colNumberOfProducts;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 1. Đăng ký bộ điều khiển vào Global registry
        SomeGlobal.setAllShopController(this);

        // 2. Ánh xạ các cột cơ bản dựa vào Model tĩnh ShopItem
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colShopName.setCellValueFactory(new PropertyValueFactory<>("shopName"));
        colNumberOfProducts.setCellValueFactory(new PropertyValueFactory<>("productCount"));

        // 3. Tạo Số Thứ Tự (STT) tự động tăng tăng theo vị trí dòng
        colNo.setCellValueFactory(cellData -> {
            int index = userTable.getItems().indexOf(cellData.getValue());
            return new SimpleStringProperty(String.valueOf(index + 1));
        });

        System.out.println("[AllShopController] Đã map thành công các fx:id và cấu hình bảng hiển thị cửa hàng.");
    }

    /**
     * Hàm nhận dữ liệu từ Handler (luồng Mạng) truyền xuống để render dữ liệu lên TableView
     */
    public void updateTableData(List<ShopItem> shopList) {
        if (shopList != null) {
            userTable.setItems(FXCollections.observableArrayList(shopList));
            userTable.refresh();
        }
    }

    /**
     * Lớp tĩnh phụ trợ (DTO) để bọc chuẩn xác cấu trúc dữ liệu gửi về từ Server
     */
    public static class ShopItem {
        private String id;
        private String email;
        private String name;
        private String shopName;
        private String status;
        private int productCount;

        // Getters & Setters bắt buộc phải có đúng chuẩn để PropertyValueFactory hoạt động
        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getShopName() { return shopName; }
        public void setShopName(String shopName) { this.shopName = shopName; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }

        public int getProductCount() { return productCount; }
        public void setProductCount(int productCount) { this.productCount = productCount; }
    }
}