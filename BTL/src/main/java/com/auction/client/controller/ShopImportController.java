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

public class ShopImportController {

    @FXML private TextField name;
    @FXML private TextField price;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private TextArea moreInfo;
    @FXML private Label successLabel;
    @FXML private ImageView productImageView;
    @FXML private Label uploadLabel;

    private String selectedImageBase64 = null;
    private String editingProductId = null;
    private static ShopImportController instance;

    public void updateSuccessLabel(String message, boolean isSuccess) {
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        successLabel.setText(message);

        if (isSuccess) {
            successLabel.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            // deleteAllClicked(null); // Tùy mày, nếu muốn xóa form sau khi lưu thành công
        } else {
            successLabel.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");
        }
    }

    public ShopImportController() {
        instance = this;
    }

    public static ShopImportController getInstance() {
        return instance;
    }

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
        NavigationService.setCenterView("/com/auction/client/view/Shop.fxml");
    }

    @FXML
    public void addProductClicked(ActionEvent event) {
        try {
            // Kiểm tra xem đang là thêm mới hay sửa
            String id = (editingProductId == null) ? Generate_id_and_timecreated.generateFullInfo()[0] : editingProductId;
            LocalDateTime timeCreated = Generate_id_and_timecreated.getCurrentTimestamp2();

            Product product = new Product();
            product.setId(id);
            product.setName(name.getText());
            product.setCategory(categoryComboBox.getValue());
            product.setStartPrice(Double.parseDouble(price.getText()));
            product.setDescription(moreInfo.getText());
            product.setImageBase64(selectedImageBase64); // Gửi base64 mới lên để server upload cloud
            product.setTimeCreated(timeCreated);
            product.setStatus(AVAILABLE);
            product.setStepPrice(product.getStartPrice() * 0.1);

            if (editingProductId == null) {
                RequestSender.sendImportProductRequest(product);
            } else {
                RequestSender.sendEditProductRequest(product);
            }

            successLabel.setText("Đang gửi dữ liệu lên Server...");
            successLabel.setVisible(true);

        } catch (Exception e) {
            updateSuccessLabel("Lỗi: " + e.getMessage(), false);
        }
    }

    // Đổ dữ liệu vào form khi bấm "Sửa" từ danh sách
    public void fillProductData(Product product) {
        editingProductId = product.getId();
        name.setText(product.getName());
        price.setText(String.valueOf(product.getStartPrice()));
        moreInfo.setText(product.getDescription());
        categoryComboBox.setValue(product.getCategory());

        // QUAN TRỌNG: Load ảnh từ URL (imagePath) chứ không dùng Base64 nữa
        if (product.getImagePath() != null && product.getImagePath().startsWith("http")) {
            Image image = new Image(product.getImagePath(), true);
            productImageView.setImage(image);
            productImageView.setVisible(true);
            uploadLabel.setVisible(false);
        } else {
            productImageView.setImage(null);
            uploadLabel.setVisible(true);
        }
    }

    @FXML
    public void deleteAllClicked(ActionEvent event) {
        editingProductId = null; // Reset trạng thái sửa về thêm mới
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
}