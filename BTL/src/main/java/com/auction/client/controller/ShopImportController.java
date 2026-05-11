package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.Generate_id_and_timecreated;
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

import static com.auction.common.model.product.ProductStatus.AVAILABLE;

public class ShopImportController {

    @FXML private TextField name;
    @FXML private TextField price;
    @FXML private ComboBox<String> categoryComboBox; // Đặt fx:id trong FXML là categoryComboBox
    @FXML private TextArea moreInfo;

    @FXML
    public void initialize() {
        // Khởi tạo danh sách danh mục (nếu cần)
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Điện tử", "Thời trang", "Gia dụng", "Xe cộ", "Khác"
        ));
    }

    @FXML
    public void handleBackClicked(ActionEvent event) throws IOException {
        NavigationService.setCenterView("/com/auction/client/view/Shop.fxml");
    }

    @FXML
    public void addProductClicked(ActionEvent event) {
        try {
            // 1. Tạo ID và Thời gian tự động
            String[] info = Generate_id_and_timecreated.generateFullInfo();
            String id = info[0];
            LocalDateTime timeCreated=Generate_id_and_timecreated.getCurrentTimestamp2();

            // 2. Lấy dữ liệu từ giao diện
            String productName = name.getText();
            double startPrice = Double.parseDouble(price.getText());
            String category = categoryComboBox.getValue();
            String description = moreInfo.getText();

            // 3. Tạo đối tượng Product (Dựa theo sơ đồ thực thể của bạn)
            Product product = new Product();
            product.setId(id);
            product.setTimeCreated(timeCreated);
            product.setName(productName);
            product.setCategory(category);
            product.setStartPrice(startPrice);
            product.setCurrentPrice(startPrice); // Mặc định giá hiện tại = giá khởi điểm
            product.setStepPrice(startPrice * 0.1); // Ví dụ bước giá mặc định 10%
            product.setDescription(description);
            product.setStatus(AVAILABLE);
            // product.setOwner("Tên User hiện tại"); // Thêm nếu bạn có lưu User session

            // 4. Gửi yêu cầu qua Network
            RequestSender.sendImportProductRequest(product);

            // 5. Thông báo hoặc chuyển trang
            System.out.println("Đã gửi yêu cầu nhập hàng: " + productName);
            handleBackClicked(null);

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
}