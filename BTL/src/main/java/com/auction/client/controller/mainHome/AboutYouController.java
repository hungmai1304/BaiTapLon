package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.SnapshotParameters;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class AboutYouController {
    @FXML
    private Label userName;

    @FXML
    private Label userEmail;

    @FXML
    private Label userPassword;

    @FXML
    private Label userState;

    @FXML
    private ImageView userAvatar;

    @FXML
    public void initialize() {
        ControllerRegistry.register("AboutYouController", this);

        // Bo tròn avatar trên UI
        Circle clip = new Circle(150, 150, 150);
        userAvatar.setClip(clip);

        // Lấy thông tin user hiện tại từ Global
        User user = SomeGlobal.getCurrentUser();

        if (user != null) {
            // Đổ dữ liệu vào các Label
            if (userName != null) userName.setText(user.getUsername());
            if (userEmail != null) userEmail.setText(user.getEmail());

            // Password thường nên che đi hoặc để dạng hash, nhưng theo FXML của bố đang để hiện
            if (userPassword != null) userPassword.setText(user.getPassword());

            // Trạng thái (Tùy logic của bố, ở đây ví dụ là "Đang hoạt động")
            if (userState != null) userState.setText("Đang hoạt động");

            // Load Avatar
            loadAvatar(user.getAvatar());
        }
    }

    private void loadAvatar(String base64) {
        if (base64 != null && !base64.isEmpty()) {
            try {
                byte[] imageBytes = Base64.getDecoder().decode(base64);
                Image image = new Image(new ByteArrayInputStream(imageBytes));
                userAvatar.setImage(image);
            } catch (Exception e) {
                System.err.println("[AboutYou] Lỗi load avatar: " + e.getMessage());
            }
        }
    }

    @FXML
    public void handleUploadAvatar(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh đại diện");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showOpenDialog(null);
        if (file == null) return;

        Image originalImage = new Image(file.toURI().toString());

        // Tạo Dialog căn chỉnh
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Căn chỉnh ảnh đại diện");
        dialog.setHeaderText("Kéo ảnh để căn chỉnh vào vòng tròn");

        ButtonType saveButtonType = new ButtonType("Lưu", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        // Khung chứa ảnh để crop (250x250)
        Pane cropPane = new Pane();
        cropPane.setPrefSize(250, 250);
        cropPane.setMinSize(250, 250);
        cropPane.setMaxSize(250, 250);
        cropPane.setStyle("-fx-background-color: #f0f0f0; -fx-border-color: #ccc;");

        // Bo tròn khung preview
        Circle cropClip = new Circle(125, 125, 125);
        cropPane.setClip(cropClip);

        ImageView view = new ImageView(originalImage);
        view.setPreserveRatio(true);
        if (originalImage.getWidth() > originalImage.getHeight()) {
            view.setFitHeight(250);
        } else {
            view.setFitWidth(250);
        }

        // Cho phép kéo ảnh
        final double[] offset = new double[2];
        view.setOnMousePressed(e -> {
            offset[0] = e.getX();
            offset[1] = e.getY();
        });
        view.setOnMouseDragged(e -> {
            view.setTranslateX(view.getTranslateX() + e.getX() - offset[0]);
            view.setTranslateY(view.getTranslateY() + e.getY() - offset[1]);
        });

        cropPane.getChildren().add(view);

        // Thêm hướng dẫn trực quan (vòng tròn viền)
        Circle overlay = new Circle(125, 125, 125);
        overlay.setFill(null);
        overlay.setStroke(Color.rgb(33, 150, 243, 0.8));
        overlay.setStrokeWidth(3);
        overlay.setMouseTransparent(true);

        StackPane container = new StackPane(cropPane, overlay);
        container.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(container);

        dialog.showAndWait().ifPresent(response -> {
            if (response == saveButtonType) {
                // Chụp ảnh vùng cropPane
                SnapshotParameters params = new SnapshotParameters();
                params.setFill(Color.TRANSPARENT);
                WritableImage croppedImage = cropPane.snapshot(params, null);

                String base64 = encodeImageToBase64(croppedImage);
                if (base64 != null) {
                    Map<String, Object> data = new HashMap<>();
                    data.put("avatarBase64", base64);
                    RequestSender.send(MessageType.UPDATE_AVATAR_REQUEST, data);
                    loadAvatar(base64);
                }
            }
        });
    }

    private String encodeImageToBase64(Image image) {
        try {
            java.awt.image.BufferedImage bufferedImage = javafx.embed.swing.SwingFXUtils.fromFXImage(image, null);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(bufferedImage, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void onUpdateAvatarSuccess(String newBase64) {
        Platform.runLater(() -> {
            // Cập nhật vào Global
            User user = SomeGlobal.getCurrentUser();
            if (user != null) {
                user.setAvatar(newBase64);
            }
            loadAvatar(newBase64);
        });
    }

    @FXML
    public void handleBackMain(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/main.fxml");
    }
}
