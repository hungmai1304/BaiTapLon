package com.auction.client.controller.mainHome;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.utils.NavigationService;
import com.auction.client.network.RequestSender;
import com.auction.protocol.MessageType;
import javafx.application.Platform;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BankController {
    @FXML
    private VBox withdraw;
    @FXML
    private VBox deposit;

    @FXML
    private TextField depositAmountField;
    @FXML
    private TextField withdrawAmountField;

    // Mapped với fx:id="balance_amount" trong file FXML
    @FXML
    private Label balance_amount;

    /**
     * Hàm tự động chạy khi giao diện Bank được nạp lên
     */
    @FXML
    public void initialize() {
        // Đăng ký controller này vào hệ thống global
        SomeGlobal.setBankController(this);

        // Hiển thị tạm thời số dư cũ từ RAM lên UI trước để tránh màn hình trống
        if (SomeGlobal.getCurrentUser() != null) {
            updateBalanceOnUI(SomeGlobal.getCurrentUser().getBalance());
        }

        // --- Cải tiến 1: Tự động yêu cầu Server cập nhật số dư mới nhất khi mở màn hình ---
        System.out.println("[BankController] Yêu cầu cập nhật số dư mới nhất...");
        RequestSender.send(MessageType.GET_BALANCE_REQUEST, null);
    }

    /**
     * Hàm dùng để các Handler từ luồng Network gọi vào để đổi số dư trên UI
     */
    public void updateBalanceOnUI(double newBalance) {
        // Đảm bảo chạy trên luồng giao diện (JavaFX Application Thread) tránh crash ứng dụng
        Platform.runLater(() -> {
            balance_amount.setText(String.format("%,.2f", newBalance)); // Định dạng hiển thị: 1,000,000.00
        });
    }

    @FXML
    public void handleWithdrawClicked(ActionEvent event) {
        withdraw.setVisible(true);
        withdraw.setManaged(true);
        deposit.setVisible(false);
        deposit.setManaged(false);
    }

    @FXML
    public void handleDepositeClicked(ActionEvent event) {
        deposit.setVisible(true);
        deposit.setManaged(true);
        withdraw.setVisible(false);
        withdraw.setManaged(false);
    }

    @FXML
    public void handleConfirmDeposit(ActionEvent event) {
        String amountText = depositAmountField.getText().trim();
        if (validateAmount(amountText)) {
            double amount = Double.parseDouble(amountText);

            // --- Cải tiến 2: Bọc dữ liệu vào Map để khớp với cách nhận data của Server ---
            Map<String, Object> reqData = new HashMap<>();
            reqData.put("data", amount); // Đưa vào key "data" đúng như Server 'data.get("data")' yêu cầu

            RequestSender.send(MessageType.DEPOSIT_REQUEST, reqData);
            depositAmountField.clear();
            RequestSender.send(MessageType.GET_BALANCE_REQUEST,null);
        }
    }

    @FXML
    public void handleConfirmWithdraw(ActionEvent event) {
        String amountText = withdrawAmountField.getText().trim();
        if (validateAmount(amountText)) {
            double amount = Double.parseDouble(amountText);

            // --- Cải tiến 3: Bọc số tiền rút tương tự như nạp ---
            Map<String, Object> reqData = new HashMap<>();
            reqData.put("data", amount);

            RequestSender.send(MessageType.WITHDRAW_REQUEST, reqData);
            withdrawAmountField.clear();
            RequestSender.send(MessageType.GET_BALANCE_REQUEST,null);
        }
    }

    @FXML
    public void handleClickedBack(ActionEvent event) throws IOException {
        NavigationService.setCenterView("/com/auction/client/view/main.fxml");
    }

    private boolean validateAmount(String amountText) {
        if (amountText.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Vui lòng nhập số tiền!");
            return false;
        }
        try {
            double amount = Double.parseDouble(amountText);
            if (amount <= 0) {
                showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số tiền phải lớn hơn 0!");
                return false;
            }
            return true;
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.ERROR, "Lỗi nhập liệu", "Số tiền không hợp lệ!");
            return false;
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}