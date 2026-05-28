package com.auction.client.handler.bidding;

import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.Response;
import com.auction.client.controller.mainHome.GlobalMarqueeController;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.controller.tiktok.BiddingController; // Import trực tiếp controller phòng đấu giá
import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
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
            String rawMessage = response.getMessage();
            if (rawMessage == null) rawMessage = "";

            // BẪY CHẶN TIN NHẮN MẬT CỦA SELLER ĐỂ BẢO VỆ POPUP CHÍNH
            if (rawMessage.contains("của bạn đã bán thành công") || rawMessage.contains("của bạn đã kết thúc")) {
                System.out.println(" [Mật thư Seller]: " + rawMessage);
                // Bỏ qua, không bật Popup để nhường chỗ cho tin nhắn Public bật Popup chuẩn!
                return;
            }
            String winnerEmail = "Không có";
            String productName = "Sản phẩm";
            double finalPrice = 0.0;
            boolean isSuccess = !rawMessage.contains("Rất tiếc") && !rawMessage.contains("Phiên đấu giá đã kết thúc");
            try {
                // 1. Bóc tách tên sản phẩm trước để làm dữ liệu đối chiếu phòng
                if (rawMessage.contains("'")) {
                    int firstQuote = rawMessage.indexOf("'");
                    int secondQuote = rawMessage.indexOf("'", firstQuote + 1);
                    if (firstQuote != -1 && secondQuote != -1) {
                        productName = rawMessage.substring(firstQuote + 1, secondQuote);
                    }
                }
                if (isSuccess) {
                    // Bóc tách Email người thắng
                    if (rawMessage.contains("Chúc mừng ") && rawMessage.contains(" đã chốt đơn")) {
                        int emailStart = rawMessage.indexOf("Chúc mừng ") + "Chúc mừng ".length();
                        int emailEnd = rawMessage.indexOf(" đã chốt đơn");
                        winnerEmail = rawMessage.substring(emailStart, emailEnd).trim();
                    }
                    // Bóc tách số tiền
                    if (rawMessage.contains("với giá ")) {
                        int priceStart = rawMessage.indexOf("với giá ") + "với giá ".length();
                        int priceEnd = rawMessage.indexOf("đ", priceStart);
                        if (priceStart != -1 && priceEnd != -1) {
                            String priceStr = rawMessage.substring(priceStart, priceEnd);
                            priceStr = priceStr.replaceAll("[.,\\s]", "").trim();
                            finalPrice = Double.parseDouble(priceStr);
                        }
                    }
                    // THANH CHỮ CHẠY TOÀN SERVER: Luôn kích hoạt cho mọi màn hình cùng đọc
                    GlobalMarqueeController.addNotification(winnerEmail, productName, finalPrice);
                }
            } catch (Exception e) {
                System.err.println("[Parser Error] Lỗi bóc tách chuỗi văn bản từ Server.");
            }
            // 2. KHỞI TẠO CỬA SỔ POPUP Ô VUÔNG (Giữ nguyên giao diện chuẩn phối dải màu của ông)
            Stage dialog = new Stage();
            dialog.initStyle(StageStyle.TRANSPARENT);
            dialog.initModality(Modality.APPLICATION_MODAL);
            VBox root = new VBox(25);
            root.setStyle("-fx-background-color: #1f293d; -fx-background-radius: 20; -fx-border-radius: 20; -fx-border-color: #2c3e50; -fx-border-width: 2; -fx-padding: 30;");
            root.setPrefWidth(460);
            root.setAlignment(Pos.TOP_CENTER);
            HBox topBar = new HBox();
            topBar.setAlignment(Pos.TOP_RIGHT);
            Button btnCloseX = new Button("✕");
            btnCloseX.setStyle("-fx-background-color: transparent; -fx-text-fill: #94a3b8; -fx-font-size: 18; -fx-cursor: hand;");
            btnCloseX.setOnAction(e -> dialog.close());
            topBar.getChildren().add(btnCloseX);
            root.getChildren().add(topBar);
            String titleText = isSuccess ? "CHÚC MỪNG PHIÊN ĐẤU GIÁ\nTHÀNH CÔNG." : "PHIÊN ĐẤU GIÁ\nĐÃ KẾT THÚC.";
            Label lblMainTitle = new Label(titleText);
            lblMainTitle.setTextFill(Color.WHITE);
            lblMainTitle.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
            lblMainTitle.setTextAlignment(TextAlignment.CENTER);
            lblMainTitle.setAlignment(Pos.CENTER);
            root.getChildren().add(lblMainTitle);
            StackPane targetGraphic = new StackPane();
            targetGraphic.setPrefSize(120, 120);
            Circle outerCircle = new Circle(45, Color.TRANSPARENT); outerCircle.setStroke(Color.web("#94a3b8")); outerCircle.setStrokeWidth(3);
            Circle midCircle = new Circle(30, Color.TRANSPARENT); midCircle.setStroke(Color.web("#94a3b8")); midCircle.setStrokeWidth(2);
            Circle bullseye = new Circle(15, Color.TRANSPARENT); bullseye.setStroke(Color.web("#e2e8f0")); bullseye.setStrokeWidth(2);
            Line arrowShaft = new Line(-40, 40, 15, -15); arrowShaft.setStroke(Color.web("#e2e8f0")); arrowShaft.setStrokeWidth(4);
            Circle statusCircle = new Circle(14, isSuccess ? Color.web("#10b981") : Color.web("#ef4444"));
            Line checkShort = new Line(-5, 0, 0, 5); checkShort.setStroke(Color.WHITE); checkShort.setStrokeWidth(2);
            Line checkLong = new Line(0, 5, 8, -5); checkLong.setStroke(Color.WHITE); checkLong.setStrokeWidth(2);
            StackPane checkmark = new StackPane(checkShort, checkLong); checkmark.setTranslateX(1.5); checkmark.setTranslateY(1);
            Line crossLine1 = new Line(-6, -6, 6, 6); crossLine1.setStroke(Color.WHITE); crossLine1.setStrokeWidth(2);
            Line crossLine2 = new Line(-6, 6, 6, -6); crossLine2.setStroke(Color.WHITE); crossLine2.setStrokeWidth(2);
            StackPane cross = new StackPane(crossLine1, crossLine2);
            StackPane statusBadge = new StackPane(statusCircle, isSuccess ? checkmark : cross); statusBadge.setTranslateX(30); statusBadge.setTranslateY(30);
            targetGraphic.getChildren().addAll(outerCircle, midCircle, bullseye, arrowShaft, statusBadge);
            root.getChildren().add(targetGraphic);

            Label lblSubtitle = new Label(isSuccess ? "Dưới đây là chi tiết phiên đấu giá." : "Không có thành viên nào ra giá.");
            lblSubtitle.setTextFill(Color.web("#94a3b8")); lblSubtitle.setStyle("-fx-font-size: 14;");
            root.getChildren().add(lblSubtitle);

            VBox infoBox = new VBox(5);
            infoBox.setPadding(new Insets(10, 0, 10, 0));
            infoBox.getChildren().addAll(
                    createStyledRow("Người thắng", winnerEmail, "#2d3748"),
                    createStyledRow("Sản phẩm", productName, "transparent"),
                    createStyledRow("Giá trúng", String.format("%,.0f đ", finalPrice), "#2d3748"),
                    createStyledRow("Bằng chữ", convertMoneyToVietnameseWords(finalPrice), "transparent")
            );
            root.getChildren().add(infoBox);
            Button btnConfirm = new Button("XÁC NHẬN");
            btnConfirm.setMaxWidth(Double.MAX_VALUE); btnConfirm.setPrefHeight(45);
            btnConfirm.setStyle("-fx-background-color: #10b981; -fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14; -fx-background-radius: 8; -fx-cursor: hand;");
            btnConfirm.setOnAction(e -> dialog.close());
            root.getChildren().add(btnConfirm);
            Scene scene = new Scene(root);
            scene.setFill(Color.TRANSPARENT);
            dialog.setScene(scene);
            dialog.centerOnScreen();
            //  BỘ LỌC TUYỆT ĐỐI: CHỈ HIỆN POPUP NẾU USER ĐANG Ở ĐÚNG PHÒNG ĐẤU GIÁ ĐÓ
            boolean isUserInThisProductRoom = false;
            try {
                Object registryController = ControllerRegistry.get("BiddingController");
                if (registryController instanceof BiddingController) {
                    BiddingController activeController = (BiddingController) registryController;

                    // Lấy nhãn hiển thị tên sản phẩm thông qua hàm Getter mới thêm công khai
                    Label lbl = activeController.getLblProductName();

                    // ĐIỀU KIỆN QUYẾT ĐỊNH: Màn hình phòng này phải đang hiển thị trên cửa sổ (getScene không null)
                    if (lbl != null && lbl.getScene() != null && lbl.getScene().getWindow() != null) {
                        Auction currentAuction = activeController.getCurrentAuctionData();
                        if (currentAuction != null && currentAuction.getProduct() != null) {
                            Product activeProduct = (Product) currentAuction.getProduct();

                            // Đối chiếu chính xác tên sản phẩm đang mở xem trên màn hình với gói tin vừa kết thúc
                            if (activeProduct.getName() != null && activeProduct.getName().trim().equals(productName.trim())) {
                                isUserInThisProductRoom = true;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("[Filter Error] Lỗi kiểm tra bộ lọc hiển thị phòng: " + e.getMessage());
            }

            // Thực thi lệnh hiển thị Popup ô vuông dựa trên bộ lọc
            if (isUserInThisProductRoom) {
                dialog.show(); // Thỏa mãn điều kiện -> Hiện ô vuông giữa màn hình
            } else {
                // Đang ở màn hình chính hoặc phòng khác -> Im lặng hoàn toàn (Chỉ chạy dải chữ trên đầu App)
                System.out.println("[UX Blocked] Đã chặn popup ô vuông của '" + productName + "' thành công do rời phòng.");
            }
        });
    }
    private HBox createStyledRow(String key, String value, String bgColor) {
        HBox row = new HBox();
        row.setPadding(new Insets(10, 15, 10, 15));
        row.setSpacing(10);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setStyle("-fx-background-color: " + bgColor + "; -fx-background-radius: 8;");
        Label lblKey = new Label(key + ":"); lblKey.setTextFill(Color.web("#94a3b8")); lblKey.setPrefWidth(100); lblKey.setStyle("-fx-font-size: 14;");
        Label lblValue = new Label(value); lblValue.setTextFill(Color.WHITE); lblValue.setWrapText(true); lblValue.setMaxWidth(240); lblValue.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        if (key.contains("Giá")) lblValue.setTextFill(Color.web("#38bdf8"));
        row.getChildren().addAll(lblKey, lblValue);
        return row;
    }
    // THUẬT TOÁN DỊCH TIỀN ĐỆ QUY MỚI: CHỐNG LỖI OUT OF BOUNDS KHI GIÁ TRỊ LÊN ĐẾN HÀNG TỶ
    private String convertMoneyToVietnameseWords(double amount) {
        if (amount <= 0) return "Không đồng";
        long number = (long) amount;

        String result = convertToWordsLong(number);
        result = result.trim().replaceAll("\\s+", " ") + " đồng";
        return result.substring(0, 1).toUpperCase() + result.substring(1);
    }
    private String convertToWordsLong(long number) {
        if (number == 0) return "";

        String[] ones = {"", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};

        if (number >= 1000000000L) {
            return convertToWordsLong(number / 1000000000L) + " tỷ " + convertToWordsLong(number % 1000000000L);
        }
        if (number >= 1000000L) {
            return convertToWordsLong(number / 1000000L) + " triệu " + convertToWordsLong(number % 1000000L);
        }
        if (number >= 1000L) {
            return convertToWordsLong(number / 1000L) + " nghìn " + convertToWordsLong(number % 1000L);
        }
        if (number >= 100L) {
            return ones[(int)(number / 100)] + " trăm " + convertToWordsLong(number % 100);
        }
        if (number >= 10) {
            String[] tens = {"", "mười", "hai mươi", "ba mươi", "bốn mươi", "năm mươi", "sáu mươi", "bảy mươi", "tám mươi", "chín mươi"};
            long t = number / 10;
            long u = number % 10;
            String uStr = "";
            if (u == 1 && t > 1) uStr = "mốt";
            else if (u == 5) uStr = "lăm";
            else if (u > 0) uStr = ones[(int)u];
            return tens[(int)t] + " " + uStr;
        }

        return ones[(int)number];
    }
}