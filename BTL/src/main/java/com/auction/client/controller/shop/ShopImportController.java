package com.auction.client.controller.shop;

import com.auction.client.network.RequestSender;
import com.auction.common.utils.Generate_id_and_timecreated;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.product.Product;
import com.auction.protocol.MessageType;
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
    @FXML private TextField wait;      // Ô nhập mới 1
    @FXML private TextField duration;  // Ô nhập mới 2
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
                "Art", "Electronics", "Fashion", "Vehicles", "Property", "Other"
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
            productImageView.setManaged(true);
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
        RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);
    }

    @FXML
    public void addProductClicked(ActionEvent event) {
        try {
            // Chỉ kiểm tra bắt buộc đối với Tên và Giá sản phẩm
            if (name.getText().trim().isEmpty() || price.getText().trim().isEmpty()) {
                updateSuccessLabel("Lỗi: Vui lòng điền Tên và Giá sản phẩm!", false);
                return;
            }

            String id = (editingProductId == null) ? Generate_id_and_timecreated.generateFullInfo()[0] : editingProductId;
            LocalDateTime timeCreated = Generate_id_and_timecreated.getCurrentTimestamp2();

            double startPrice = Double.parseDouble(price.getText().trim());

            Product product = new Product();
            product.setId(id);
            product.setName(name.getText().trim());
            product.setCategory(categoryComboBox.getValue());
            product.setStartPrice(startPrice);
            product.setDescription(moreInfo.getText().trim());
            product.setImageBase64(selectedImageBase64);
            product.setTimeCreated(timeCreated);
            product.setStatus(AVAILABLE);
            product.setStepPrice(startPrice * 0.1);

            // --- LOGIC XỬ LÝ AN TOÀN CHO DỮ LIỆU THỜI GIAN (ĐỒNG BỘ KIỂU DOUBLE) ---
            String waitText = wait.getText().trim();
            String durationText = duration.getText().trim();

            Double waitingMins = null;
            Double durationMins = null;

            // Kiểm tra ô wait: nếu có nhập và khác số 0
            if (!waitText.isEmpty()) {
                double w = Double.parseDouble(waitText);
                if (w != 0.0) {
                    waitingMins = w;
                }
            }

            // Kiểm tra ô duration: nếu có nhập và khác số 0
            if (!durationText.isEmpty()) {
                double d = Double.parseDouble(durationText);
                if (d != 0.0) {
                    durationMins = d;
                }
            }

            // Gán giá trị Double (Có thể là số cụ thể hoặc null) vào product
            product.setWaitingMinutes(waitingMins);
            product.setDurationMinutes(durationMins);
            // ----------------------------------------------------------------------

            // Gửi tin nhắn qua Network
            if (editingProductId == null) {
                RequestSender.sendImportProductRequest(product);
            } else {
                RequestSender.sendEditProductRequest(product);
            }

            updateSuccessLabel("Gửi yêu cầu thành công!", true);

        } catch (NumberFormatException e) {
            updateSuccessLabel("Lỗi: Giá hoặc thời gian nhập vào phải là định dạng số!", false);
        } catch (Exception e) {
            updateSuccessLabel("Lỗi: " + e.getMessage(), false);
        }
    }

    // Đổ dữ liệu vào form khi bấm "Sửa" từ danh sách công khai
    public void fillProductData(Product product) {
        editingProductId = product.getId();
        name.setText(product.getName());
        price.setText(String.format("%.0f", product.getStartPrice()));
        moreInfo.setText(product.getDescription());
        categoryComboBox.setValue(product.getCategory());

        // --- ĐỔ NGƯỢC DỮ LIỆU THỜI GIAN KIỂU DOUBLE (Loại bỏ phần đuôi .0 nếu là số nguyên để UI đẹp hơn) ---
        if (product.getWaitingMinutes() != null && product.getWaitingMinutes() != 0.0) {
            double w = product.getWaitingMinutes();
            if (w == (long) w) {
                wait.setText(String.format("%d", (long) w));
            } else {
                wait.setText(String.valueOf(w));
            }
        } else {
            wait.clear();
        }

        if (product.getDurationMinutes() != null && product.getDurationMinutes() != 0.0) {
            double d = product.getDurationMinutes();
            if (d == (long) d) {
                duration.setText(String.format("%d", (long) d));
            } else {
                duration.setText(String.valueOf(d));
            }
        } else {
            duration.clear();
        }
        // ----------------------------------------------------------------------

        if (product.getImagePath() != null && product.getImagePath().startsWith("http")) {
            Image image = new Image(product.getImagePath(), true);
            productImageView.setImage(image);
            productImageView.setVisible(true);
            productImageView.setManaged(true);
            uploadLabel.setVisible(false);
        }
        else if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
            try {
                byte[] imageBytes = java.util.Base64.getDecoder().decode(product.getImageBase64());
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes);
                Image image = new Image(bais);

                productImageView.setImage(image);
                productImageView.setVisible(true);
                productImageView.setManaged(true);
                uploadLabel.setVisible(false);

                this.selectedImageBase64 = product.getImageBase64();
            } catch (Exception e) {
                System.err.println("[ShopImport Error] Không thể dựng ảnh từ dữ liệu Base64!");
                e.printStackTrace();
                setDefaultUploadState();
            }
        }
        else {
            setDefaultUploadState();
        }
    }

    @FXML
    public void deleteAllClicked(ActionEvent event) {
        editingProductId = null;
        name.clear();
        price.clear();
        wait.clear();        // Xóa sạch ô nhập mới 1
        duration.clear();    // Xóa sạch ô nhập mới 2
        moreInfo.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        setDefaultUploadState();
    }

    private void setDefaultUploadState() {
        productImageView.setImage(null);
        productImageView.setVisible(false);
        productImageView.setManaged(false);
        uploadLabel.setVisible(true);
        selectedImageBase64 = null;
    }

    @FXML
    public void categoryClicked(ActionEvent event) {
    }
}