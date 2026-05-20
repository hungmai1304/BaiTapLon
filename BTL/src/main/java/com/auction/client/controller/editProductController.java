package com.auction.client.controller;

import com.auction.client.network.RequestSender;
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

    // Giữ lại đối tượng sản phẩm gốc để bảo tồn các thông tin khác (owner, timeCreated,...)
    private Product originalProduct = null;

    @FXML
    public void initialize() {
        // Updated categories to English and matched with SearchController
        categoryComboBox.setItems(FXCollections.observableArrayList(
                "Art", "Electronics", "Fashion", "Vehicles", "Property", "Other"
        ));
    }

    @FXML
    public void handleImageClicked(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm mới");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file != null) {
            Image image = new Image(file.toURI().toString());
            productImageView.setImage(image);
            productImageView.setVisible(true);
            productImageView.setManaged(true); // Đảm bảo layout tính toán kích thước khi có ảnh mới

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
        RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);
    }

    @FXML
    public void editProductClicked(ActionEvent event) {
        try {
            if (this.originalProduct == null) {
                successLabel.setVisible(true);
                successLabel.setText("Lỗi: Không tìm thấy thông tin sản phẩm cần sửa!");
                successLabel.setManaged(true);
                successLabel.setStyle("-fx-text-fill: red;");
                return;
            }

            // Cập nhật các thông tin mới do người dùng nhập vào đối tượng gốc ban đầu
            originalProduct.setName(name.getText());
            originalProduct.setCategory(categoryComboBox.getValue());

            double startPrice = Double.parseDouble(price.getText());
            originalProduct.setStartPrice(startPrice);
            originalProduct.setCurrentPrice(startPrice);
            originalProduct.setStepPrice(startPrice * 0.1);

            originalProduct.setDescription(moreInfo.getText());
            originalProduct.setStatus(AVAILABLE);

            // Nếu người dùng chọn ảnh mới -> Gửi Base64 mới lên.
            // Nếu không -> Server giữ nguyên file cũ trên ổ cứng (selectedImageBase64 ban đầu được giữ từ ảnh cũ hoặc giữ null)
            originalProduct.setImageBase64(selectedImageBase64);

            // Gửi toàn bộ đối tượng (Đã có sẵn thông tin Owner) lên Server
            RequestSender.sendEditProductRequest(originalProduct);

            successLabel.setVisible(true);
            successLabel.setManaged(true);
            successLabel.setText("Đã gửi yêu cầu cập nhật lên Server...");
            successLabel.setStyle("-fx-text-fill: green;");

        } catch (NumberFormatException e) {
            successLabel.setVisible(true);
            successLabel.setManaged(true);
            successLabel.setText("Lỗi: Giá phải là số!");
            successLabel.setStyle("-fx-text-fill: red;");
        } catch (Exception e) {
            e.printStackTrace();
            successLabel.setVisible(true);
            successLabel.setManaged(true);
            successLabel.setText("Lỗi hệ thống: " + e.getMessage());
            successLabel.setStyle("-fx-text-fill: red;");
        }
    }

    // Đổ dữ liệu từ màn hình Shop vào màn hình Edit
    public void fillProductData(Product product) {
        // --- 3 DÒNG NÀY ĐỂ DEBUG XEM LỖI Ở ĐÂU ---
        System.out.println("=== KIỂM TRA DỮ LIỆU KHI BẤM EDIT ===");
        System.out.println("ImagePath (Trong DB): " + product.getImagePath());
        System.out.println("ImageBase64: " + (product.getImageBase64() != null ? "Có dữ liệu (Độ dài: " + product.getImageBase64().length() + ")" : "RỖNG (NULL)!!!"));

        // Lưu lại trọn vẹn đối tượng sản phẩm (Bao gồm cả trường Owner bên trong)
        this.originalProduct = product;

        name.setText(product.getName());
        price.setText(String.format("%.0f", product.getStartPrice()));
        moreInfo.setText(product.getDescription());
        categoryComboBox.setValue(product.getCategory());

        // --- GIỮ NGUYÊN: Hỗ trợ load ảnh từ URL Cloudinary (Nếu có sẵn) ---
        if (product.getImagePath() != null && product.getImagePath().startsWith("http")) {
            Image image = new Image(product.getImagePath(), true);
            productImageView.setImage(image);
            productImageView.setVisible(true);
            productImageView.setManaged(true);
            uploadLabel.setVisible(false);
        }
        // --- THÊM MỚI: Xử lý hiển thị ảnh từ chuỗi Base64 do Server Local đọc và trả về ---
        else if (product.getImageBase64() != null && !product.getImageBase64().isEmpty()) {
            try {
                // 1. Giải mã chuỗi Base64 nhận từ Server thành mảng byte
                byte[] imageBytes = java.util.Base64.getDecoder().decode(product.getImageBase64());

                // 2. Đọc luồng byte dữ liệu nhị phân thành JavaFX Image
                java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(imageBytes);
                Image image = new Image(bais);

                // 3. Hiển thị lên ImageView cục bộ
                productImageView.setImage(image);
                productImageView.setVisible(true);
                productImageView.setManaged(true);
                uploadLabel.setVisible(false);

                // Đồng bộ lại biến toàn cục để phòng trường hợp user bấm Save luôn mà không thay đổi ảnh
                this.selectedImageBase64 = product.getImageBase64();

            } catch (Exception e) {
                System.err.println("[Client Error] Không thể dựng ảnh từ dữ liệu Base64!");
                e.printStackTrace(); // In ra chi tiết lỗi giải mã (nếu có)
                setDefaultUploadState();
            }
        }
        // --- Trường hợp không có dữ liệu ảnh nào ---
        else {
            setDefaultUploadState();
        }
    }

    @FXML
    public void deleteAllClicked(ActionEvent event) {
        name.clear();
        price.clear();
        moreInfo.clear();
        categoryComboBox.getSelectionModel().clearSelection();
        setDefaultUploadState();
    }

    // Hàm phụ trợ cô lập logic reset giao diện upload ảnh để tái sử dụng
    private void setDefaultUploadState() {
        productImageView.setImage(null);
        productImageView.setVisible(false);
        productImageView.setManaged(false);
        uploadLabel.setVisible(true);
        selectedImageBase64 = null;
    }
    @FXML
    public void handleDeleteProduct(ActionEvent event) {
        if (originalProduct == null) {
            successLabel.setVisible(true);
            successLabel.setManaged(true);
            successLabel.setText("Lỗi: Không tìm thấy sản phẩm!");
            successLabel.setStyle("-fx-text-fill: red;");
            return;
        }

        RequestSender.sendDeleteProductRequest(originalProduct.getId());

        // Hiện thông báo xóa thành công
        successLabel.setVisible(true);
        successLabel.setManaged(true);
        successLabel.setText("Đã xóa sản phẩm thành công!");
        successLabel.setStyle("-fx-text-fill: #4caf50;");

        // Xóa form
        deleteAllClicked(null);

        new Thread(() -> {
            try {
                Thread.sleep(5000);
                javafx.application.Platform.runLater(() -> {
                    successLabel.setVisible(false);
                    successLabel.setManaged(false);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }
}