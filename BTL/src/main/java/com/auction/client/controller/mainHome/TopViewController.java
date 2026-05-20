package com.auction.client.controller.mainHome;

import com.auction.client.controller.mainHome.GlobalMarqueeController; // Import lớp quản lý chữ chạy
import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;  // Thêm import này
import javafx.scene.layout.Pane;  // Thêm import này
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class TopViewController {
    @FXML
    private Label clock;
    @FXML
    private Label balance;
    @FXML
    private Label top_username;
    @FXML
    private ImageView top_avatar;

    // --- CHỈ THÊM: Biến điều khiển dải chữ chạy toàn Server ---
    @FXML
    private HBox marqueeContainer;
    @FXML
    private Pane textViewPane;

    @FXML
    public void initialize() {
        SomeGlobal.setTopViewController(this);

        // --- CHỈ THÊM: Kích hoạt cỗ máy chữ chạy khi thanh Top hiển thị ---
        GlobalMarqueeController.initializeMarquee(marqueeContainer, textViewPane);

        // Bo tròn avatar
        if (top_avatar != null) {
            Circle clip = new Circle(22.5, 22.5, 22.5);
            top_avatar.setClip(clip);
        }

        // 1. Tự động đồng bộ số dư (Binding) với ClientContext
        if (balance != null) {
            balance.textProperty().bind(
                    ClientContext.getInstance().userBalanceProperty().asString("%,.2f")
            );
        }

        // 2. Load thông tin user
        User user = SomeGlobal.getCurrentUser();
        if (user != null) {
            if (top_username != null) top_username.setText(user.getUsername());
            updateAvatar(user.getAvatar());
        }

        // 3. Kích hoạt đồng hồ tự động chạy
        initClock();

        // 4. Chủ động gửi yêu cầu lấy số dư mới nhất từ Server khi thanh Top vừa hiển thị
        RequestSender.send(MessageType.GET_BALANCE_REQUEST, null);
    }

    public void updateAvatar(String base64) {
        if (base64 != null && !base64.isEmpty() && top_avatar != null) {
            Platform.runLater(() -> {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(base64);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    top_avatar.setImage(image);
                } catch (Exception e) {
                    System.err.println("[TopView] Lỗi load avatar: " + e.getMessage());
                }
            });
        }
    }

    private void initClock() {
        if (clock == null) return;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    if (clock != null) {
                        clock.setText(LocalTime.now().format(dtf));
                    }
                }),
                new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}