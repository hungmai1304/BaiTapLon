package com.auction.client.handler;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.Response;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

@ResponseHandler(type = "AUCTION_RESULT_NOTIFICATION")
public class AuctionResultClientHandler implements IClientHandler {

    @Override
    public void handle(Response response) {
        Platform.runLater(() -> {
            // 1. EXTRACT DATA FROM THE BACKEND
            String rawMessage = response.getMessage();
            if (rawMessage == null) rawMessage = "";

            // DATA PARSER (NO EMOJIS USED IN CODE LOGIC)
            String winnerEmail = "None";
            String productName = "Product";
            double finalPrice = 0.0;

            // Check success based on text (avoiding emojis in comparison)
            boolean isSuccess = !rawMessage.contains("Rất tiếc") && !rawMessage.contains("Phiên đấu giá đã kết thúc");

            try {
                if (isSuccess) {
                    // Extract Winner's Email (between "Chúc mừng " and " đã chốt đơn")
                    if (rawMessage.contains("Chúc mừng ") && rawMessage.contains(" đã chốt đơn")) {
                        int emailStart = rawMessage.indexOf("Chúc mừng ") + "Chúc mừng ".length();
                        int emailEnd = rawMessage.indexOf(" đã chốt đơn");
                        winnerEmail = rawMessage.substring(emailStart, emailEnd).trim();
                    }

                    // Extract the final price (between "với giá " and "đ!")
                    if (rawMessage.contains("với giá ")) {
                        int priceStart = rawMessage.indexOf("với giá ") + "với giá ".length();
                        int priceEnd = rawMessage.indexOf("đ", priceStart);
                        if (priceStart != -1 && priceEnd != -1) {
                            String priceStr = rawMessage.substring(priceStart, priceEnd);
                            // Normalize price string
                            priceStr = priceStr.replaceAll("[.,\\s]", "").trim();
                            finalPrice = Double.parseDouble(priceStr);
                        }
                    }
                }

                // Extract Product Name (between single quotes ' ')
                if (rawMessage.contains("'")) {
                    int firstQuote = rawMessage.indexOf("'");
                    int secondQuote = rawMessage.indexOf("'", firstQuote + 1);
                    if (firstQuote != -1 && secondQuote != -1) {
                        productName = rawMessage.substring(firstQuote + 1, secondQuote);
                    }
                }
            } catch (Exception e) {
                System.err.println("[Parser Error] Error parsing notification details.");
            }

            // 2. CREATE A TRANSPARENT MODAL POPUP STAGE
            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.initModality(Modality.APPLICATION_MODAL);

            // 3. MAIN CARD CONTAINER (MODERN DARK STYLE)
            VBox root = new VBox(25);
            root.setStyle("-fx-background-color: #1f293d; " + // Dark grey background
                    "-fx-background-radius: 20; " +     // rounded corners
                    "-fx-border-radius: 20; " +
                    "-fx-border-color: #2c3e50; " +    // subtle border
                    "-fx-border-width: 2; " +
                    "-fx-padding: 30;");                // padding
            root.setPrefWidth(460);                           // fixed width
            root.setAlignment(Pos.TOP_CENTER); // Title at the top

            // X Close Button
            HBox topBar = new HBox();
            topBar.setAlignment(Pos.TOP_RIGHT);
            Button btnCloseX = new Button("✕");
            btnCloseX.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 18; -fx-cursor: hand;");
            btnCloseX.setOnAction(e -> dialog.close());
            topBar.getChildren().add(btnCloseX);
            root.getChildren().add(topBar);

            // 4. LARGE STATUS ANNOUNCEMENT TITLE AT THE VERY TOP (TWO LINES)
            String titleText = isSuccess ? "CHÚC MỪNG PHIÊN ĐẤU GIÁ\nTHÀNH CÔNG." : "PHIÊN ĐẤU GIÁ\nĐÃ KẾT THÚC.";
            Label lblMainTitle = new Label(titleText);
            lblMainTitle.setTextFill(Color.WHITE);
            lblMainTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold;"); // Sized for the card width
            lblMainTitle.setTextAlignment(TextAlignment.CENTER);
            lblMainTitle.setAlignment(Pos.CENTER);
            root.getChildren().add(lblMainTitle);

            // 5. THE TARGET GRAPHIC WITH CORRECTED STATUS BADGE (NO Symbol Characters Are Used)
            StackPane targetGraphic = new StackPane();
            targetGraphic.setPrefSize(120, 120);

            // Target rings
            Circle outerCircle = new Circle(45, Color.TRANSPARENT); outerCircle.setStroke(Color.web("#94a3b8")); outerCircle.setStrokeWidth(3);
            Circle midCircle = new Circle(30, Color.TRANSPARENT); midCircle.setStroke(Color.web("#94a3b8")); midCircle.setStrokeWidth(2);
            Circle bullseye = new Circle(15, Color.TRANSPARENT); bullseye.setStroke(Color.web("#e2e8f0")); bullseye.setStrokeWidth(2);

            // Arrow shaft
            Line arrowShaft = new Line(-40, 40, 15, -15); arrowShaft.setStroke(Color.web("#e2e8f0")); arrowShaft.setStrokeWidth(4);

            // Corrected Status Badge - Made with shapes only
            Circle statusCircle = new Circle(14, isSuccess ? Color.web("#10b981") : Color.web("#ef4444")); // Green or Red

            // Checkmark with shapes
            Line checkShort = new Line(-5, 0, 0, 5); checkShort.setStroke(Color.WHITE); checkShort.setStrokeWidth(2);
            Line checkLong = new Line(0, 5, 8, -5); checkLong.setStroke(Color.WHITE); checkLong.setStrokeWidth(2);
            StackPane checkmark = new StackPane(checkShort, checkLong);
            checkmark.setTranslateX(1.5); checkmark.setTranslateY(1);

            // Cross (X) with shapes
            Line crossLine1 = new Line(-6, -6, 6, 6); crossLine1.setStroke(Color.WHITE); crossLine1.setStrokeWidth(2);
            Line crossLine2 = new Line(-6, 6, 6, -6); crossLine2.setStroke(Color.WHITE); crossLine2.setStrokeWidth(2);
            StackPane cross = new StackPane(crossLine1, crossLine2);

            // Display checkmark on success, cross on failure (all shape based)
            StackPane statusBadge = new StackPane(statusCircle, isSuccess ? checkmark : cross);
            statusBadge.setTranslateX(30); statusBadge.setTranslateY(30);

            targetGraphic.getChildren().addAll(outerCircle, midCircle, bullseye, arrowShaft, statusBadge);
            root.getChildren().add(targetGraphic);

            // Extra subtitle below graphic
            Label lblSubtitle = new Label(isSuccess ? "Dưới đây là chi tiết phiên đấu giá." : "Không có thành viên nào ra giá.");
            lblSubtitle.setTextFill(Color.web("#94a3b8"));
            lblSubtitle.setStyle("-fx-font-size: 14;");
            root.getChildren().add(lblSubtitle);

            // 6. DETAILED GRID INFO BOARD (Winner, Product, Price, Words in order)
            VBox infoBox = new VBox(5); // container to hold rows
            infoBox.setPadding(new Insets(10, 0, 10, 0));

            infoBox.getChildren().addAll(
                    createStyledRow("Người thắng", winnerEmail, "#2d3748"),
                    createStyledRow("Sản phẩm", productName, "transparent"),
                    createStyledRow("Giá trúng", String.format("%,.0f đ", finalPrice), "#2d3748"),
                    createStyledRow("Bằng chữ", convertMoneyToVietnameseWords(finalPrice), "transparent")
            );
            root.getChildren().add(infoBox);

            // 7. CONFIRM BUTTON AT THE BOTTOM
            Button btnConfirm = new Button("XÁC NHẬN");
            btnConfirm.setMaxWidth(Double.MAX_VALUE);
            btnConfirm.setPrefHeight(45);
            btnConfirm.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 8; -fx-cursor: hand;");
            btnConfirm.setOnAction(e -> dialog.close());
            root.getChildren().add(btnConfirm);

            // 8. SETUP SCENE AND SHOW THE DIALOG
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);

            // Center the popup on the screen
            dialog.centerOnScreen();
            dialog.show();
        });
    }

    /**
     * Helper to create a structured grid row with a background
     */
    private HBox createStyledRow(String key, String value, String bgColor) {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");

        Label lblKey = new Label(key + ":"); // Key:
        lblKey.setTextFill(Color.web("#94a3b8"));
        lblKey.setPrefWidth(100);
        lblKey.setStyle("-fx-font-size: 14;");

        Label lblValue = new Label(value);
        lblValue.setTextFill(Color.WHITE);
        lblValue.setWrapText(true);
        lblValue.setMaxWidth(240);
        lblValue.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");

        if (key.contains("Giá")) lblValue.setTextFill(Color.web("#38bdf8")); // Price in Cyan

        row.getChildren().addAll(lblKey, lblValue);
        return row;
    }

    private String convertMoneyToVietnameseWords(double amount) {
        if (amount <= 0) return "Không đồng";
        long number = (long) amount;
        String[] ones = {"", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
        String[] tens = {"", "mười", "hai mươi", "ba mươi", "bốn mươi", "năm mươi", "sáu mươi", "bảy mươi", "tám mươi", "chín mươi"};
        StringBuilder words = new StringBuilder();
        if ((number / 1000000) > 0) { words.append(ones[(int)(number / 1000000)]).append(" triệu "); number %= 1000000; }
        if ((number / 1000) > 0) {
            long nghin = number / 1000;
            if (nghin < 10) words.append(ones[(int)nghin]).append(" nghìn ");
            else words.append(tens[(int)(nghin / 10)]).append(" ").append(ones[(int)(nghin % 10)]).append(" nghìn ");
            number %= 1000;
        }
        if (number > 0) {
            if (number >= 100) { words.append(ones[(int)(number / 100)]).append(" trăm "); number %= 100; }
            if (number > 0) words.append(tens[(int)(number / 10)]).append(" ").append(ones[(int)(number % 10)]);
        }
        String result = words.toString().trim().replaceAll("\\s+", " ") + " đồng";
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
}