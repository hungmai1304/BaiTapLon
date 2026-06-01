package com.auction.client.controller.mainHome;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.util.Duration;
import java.util.LinkedList;
import java.util.Queue;
import java.util.logging.Logger;

public class GlobalMarqueeController {
private static final Logger LOGGER = Logger.getLogger(GlobalMarqueeController.class.getName());
    private static class MarqueeData {
        String username;
        String productName;
        double price;

        MarqueeData(String username, String productName, double price) {
            this.username = username;
            this.productName = productName;
            this.price = price;
        }
    }

    private static final Queue<MarqueeData> notificationQueue = new LinkedList<>();
    private static boolean isRunning = false;

    private static HBox marqueeContainer;
    private static Pane textViewPane;

    public static void initializeMarquee(HBox container, Pane viewPane) {
        marqueeContainer = container;
        textViewPane = viewPane;

        if (marqueeContainer != null) {
            marqueeContainer.setVisible(false);
            marqueeContainer.setManaged(false);
        }

        if (textViewPane != null) {
            Rectangle clip = new Rectangle();
            clip.widthProperty().bind(textViewPane.widthProperty());
            clip.heightProperty().bind(textViewPane.heightProperty());
            textViewPane.setClip(clip);
        }
    }

    public static synchronized void addNotification(String username, String productName, double price) {
        Platform.runLater(() -> {
            notificationQueue.add(new MarqueeData(username, productName, price));
            if (!isRunning) {
                processNext();
            }
        });
    }

    private static void processNext() {
        if (notificationQueue.isEmpty()) {
            isRunning = false;
            if (marqueeContainer != null) {
                marqueeContainer.setVisible(false);
                marqueeContainer.setManaged(false);
            }
            return;
        }

        isRunning = true;
        marqueeContainer.setVisible(true);
        marqueeContainer.setManaged(true);

        MarqueeData data = notificationQueue.poll();

        // ĐÃ NÂNG FONT SIZE LÊN 18 CHO CHỮ TO RÕ RÀNG HƠN
        Text t1 = new Text("Chúc mừng người dùng "); t1.setFill(Color.WHITE); t1.setStyle("-fx-font-size: 18;");
        Text tUser = new Text(data.username); tUser.setFill(Color.web("#38bdf8")); tUser.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        Text t2 = new Text(" đã đấu giá thành công sản phẩm "); t2.setFill(Color.WHITE); t2.setStyle("-fx-font-size: 18;");
        Text tProd = new Text(data.productName); tProd.setFill(Color.web("#38bdf8")); tProd.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        Text t3 = new Text(" với giá " + String.format("%,.0f", data.price) + " đ"); t3.setFill(Color.WHITE); t3.setStyle("-fx-font-size: 18;");

        TextFlow textFlow = new TextFlow(t1, tUser, t2, tProd, t3);
        textFlow.setPrefWidth(Region.USE_COMPUTED_SIZE);
        // ĐÃ XÓA BỎ DÒNG LỖI setAlignment TẠI ĐÂY GÂY ĐỎ CODE

        textViewPane.getChildren().clear();
        textViewPane.getChildren().add(textFlow);

        textViewPane.layout();

        double paneWidth = textViewPane.getWidth() > 0 ? textViewPane.getWidth() : 600;
        double textWidth = textFlow.getBoundsInLocal().getWidth();

        textFlow.setTranslateX(paneWidth);

        TranslateTransition animation = new TranslateTransition();
        animation.setNode(textFlow);
        animation.setDuration(Duration.millis((paneWidth + textWidth) * 10));
        animation.setFromX(paneWidth);
        animation.setToX(-textWidth);

        animation.setOnFinished(event -> processNext());
        animation.play();
    }
}