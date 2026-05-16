package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class TopViewController {
    @FXML
    private Label clock;
    @FXML
    private Label balance;

    @FXML
    public void initialize() {
        // 1. Tự động đồng bộ số dư (Binding) với ClientContext
        if (balance != null) {
            balance.textProperty().bind(
                    ClientContext.getInstance().userBalanceProperty().asString("%,.2f")
            );
        }

        // 2. Kích hoạt đồng hồ tự động chạy
        initClock();

        // 3. Chủ động gửi yêu cầu lấy số dư mới nhất từ Server khi thanh Top vừa hiển thị
        RequestSender.send(MessageType.GET_BALANCE_REQUEST, null);
    }

    /**
     * Khởi tạo bộ đếm thời gian thực cho đồng hồ (Định dạng HH:mm:ss)
     */
    private void initClock() {
        if (clock == null) return;

        // Định dạng hiển thị: Giờ:Phút:Giây (Ví dụ: 21:45:02)
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss");

        // Tạo bộ đếm lặp lại sau mỗi 1 giây
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> {
                    if (clock != null) {
                        clock.setText(LocalTime.now().format(dtf));
                    }
                }),
                new KeyFrame(Duration.seconds(1))
        );

        // Thiết lập chạy vô hạn và bắt đầu kích hoạt
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
}