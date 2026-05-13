package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.common.utils.Generate_id_and_timecreated;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import java.io.IOException;
import java.time.LocalDateTime;
import javafx.scene.control.Label;

import static com.auction.common.model.product.ProductStatus.AVAILABLE;

public class editProductController {

    @FXML private TextField name;
    @FXML private TextField price;
    @FXML private ComboBox<String> categoryComboBox; // Đặt fx:id trong FXML là categoryComboBox
    @FXML private TextArea moreInfo;
    @FXML private Label successLabel;


    @FXML
    public void initialize() {
        // Khởi tạo danh sách danh mục (nếu cần)
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Điện tử", "Thời trang", "Gia dụng", "Xe cộ", "Khác"
        ));
    }

    @FXML
    public void handleBackClicked(ActionEvent event) throws IOException {
        NavigationService.setCenterView("/com/auction/client/view/ShopSell.fxml");
    }

    @FXML
    public void addProductClicked(ActionEvent event) {
        try {
            //SỬA LỖI: Lấy cái ID gốc đã lưu lúc fillData ra xài, không tạo mới
            if (this.editingProductId == null) {
                System.err.println("Lỗi: Không tìm thấy ID của sản phẩm cần sửa!");
                return;
            }
            String id = this.editingProductId;

            // 2. Lấy dữ liệu từ giao diện
            String productName = name.getText();
            double startPrice = Double.parseDouble(price.getText());
            String category = categoryComboBox.getValue();
            String description = moreInfo.getText();

            // 3. Tạo đối tượng Product mang id c
            Product product = new Product();
            product.setId(id); // Nhét ID cũ vào đây

            // (Không cần set TimeCreated vì Server sẽ móc từ Database lên lấy cái thời gian gốc)
            product.setName(productName);
            product.setCategory(category);
            product.setStartPrice(startPrice);
            product.setCurrentPrice(startPrice);
            product.setStepPrice(startPrice * 0.1);
            product.setDescription(description);
            product.setStatus(AVAILABLE);

            // 4. Gửi yêu cầu qua Network
            RequestSender.sendEditProductRequest(product);

            // 5. Thông báo (Sửa lại chữ cho chuẩn, vì gửi đi chưa chắc đã lưu thành công)
            successLabel.setVisible(true);
            successLabel.setManaged(true);
            successLabel.setText("Đã gửi yêu cầu lưu thành công!");

            // Xóa form
            deleteAllClicked(null);

        } catch (NumberFormatException e) {
            System.err.println("Lỗi: Giá nhập vào phải là số!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void deleteAllClicked(ActionEvent event) {
        name.clear();
        price.clear();
        moreInfo.clear();
        categoryComboBox.getSelectionModel().clearSelection();
    }

    @FXML
    public void categoryClicked(ActionEvent event) {
        // Xử lý khi chọn danh mục nếu cần
    }

    // Nằm ngoài tất cả các hàm, ngay trong class
    private String editingProductId = null;

    public void fillProductData(Product product) {
        editingProductId = product.getId();
        name.setText(product.getName());
        price.setText(String.valueOf(product.getStartPrice()));
        moreInfo.setText(product.getDescription());
        categoryComboBox.setValue(product.getCategory());
    }
}