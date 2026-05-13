package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.common.utils.Generate_id_and_timecreated;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static com.auction.common.model.product.ProductStatus.AVAILABLE;

public class editProductController {

    @FXML private TextField name;
    @FXML private TextField price;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextArea moreInfo;
    @FXML private Label successLabel;
    @FXML private ImageView productImageView;
    @FXML private Label uploadLabel;

    private String selectedImageBase64 = null;
    private String editingProductId = null;

    @FXML
    public void initialize() {
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Điện tử", "Thời trang", "Gia dụng", "Xe cộ", "Khác"
        ));
    }

    @FXML
    public void handleImageClicked(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            productImageView.setImage(image);
            productImageView.setVisible(true);
            uploadLabel.setVisible(false);
            try {
                byte[] fileContent = java.nio.file.Files.readAllBytes(file.toPath());
                selectedImageBase64 = java.util.Base64.getEncoder().encodeToString(fileContent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    public void handleBackClicked(ActionEvent event) throws IOException {
        NavigationService.setCenterView("/com/auction/client/view/ShopSell.fxml");
    }

    @FXML
    public void addProductClicked(ActionEvent event) {
        try {
            if (this.editingProductId == null) {
                System.err.println("Lỗi: Không tìm thấy ID của sản phẩm cần sửa!");
                return;
            }
            String id = this.editingProductId;

            String productName = name.getText();
            double startPrice = Double.parseDouble(price.getText());
            String category = categoryComboBox.getValue();
            String description = moreInfo.getText();

            Product product = new Product();
            product.setId(id);
            product.setName(productName);
            product.setCategory(category);
            product.setStartPrice(startPrice);
            product.setCurrentPrice(startPrice);
            product.setStepPrice(startPrice * 0.1);
            product.setDescription(description);
            product.setStatus(AVAILABLE);
            product.setImageBase64(selectedImageBase64);

            RequestSender.sendEditProductRequest(product);

            successLabel.setVisible(true);
            successLabel.setManaged(true);
            successLabel.setText("✅ Đã gửi yêu cầu lưu thành công!");
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
        productImageView.setImage(null);
        productImageView.setVisible(false);
        uploadLabel.setVisible(true);
        selectedImageBase64 = null;
    }

    @FXML
    public void categoryClicked(ActionEvent event) {
    }

    public void fillProductData(Product product) {
        editingProductId = product.getId();
        name.setText(product.getName());
        price.setText(String.format("%.0f", product.getStartPrice()));
        moreInfo.setText(product.getDescription());
        categoryComboBox.setValue(product.getCategory());

        // Hiển thị ảnh cũ từ base64
        if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(product.getImageBase64());
            Image image = new Image(new java.io.ByteArrayInputStream(imageBytes));
            productImageView.setImage(image);
            productImageView.setVisible(true);
            uploadLabel.setVisible(false);
        }
    }
}