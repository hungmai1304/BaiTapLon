package com.auction.client.controller.mainHome;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.user.User;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

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
    public void initialize() {
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
        }
    }

    @FXML
    public void handleBackMain(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/main.fxml");
    }


}
