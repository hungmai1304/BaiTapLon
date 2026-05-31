package com.auction.client.controller.mainHome;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.user.User;
import com.auction.protocol.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import java.time.LocalTime;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.logging.Logger;

public class MainController {
private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());
    @FXML
    private GridPane gidpane_main;

    @FXML
    private AnchorPane balance_anchorpane;
    @FXML
    private Label HI_USER_NAME;
    @FXML private Label shop_number_product;
    @FXML private Label about_you_name;
    @FXML private Label balance_main;
    @FXML private AnchorPane shopAnchorPane;
    @FXML private ImageView main_avatar;

    @FXML
    public void initialize() {
        User user = SomeGlobal.getCurrentUser();
        SomeGlobal.setMainController(this);

        if (main_avatar != null) {
            Circle clip = new Circle(45, 45, 45);
            main_avatar.setClip(clip);
        }

        if (user != null && HI_USER_NAME != null) {
            if (user instanceof com.auction.common.model.user.Bidder) {
                if (shopAnchorPane != null) {
                    shopAnchorPane.setVisible(false);
                }
            }

            String username = user.getUsername();
            about_you_name.setText(username);

            // Load Avatar
            updateAvatar(user.getAvatar());

            LocalTime now = LocalTime.now();
            int hour = now.getHour();
            String greeting;

            // Chia khung giờ để chào cho chuẩn bài
            if (hour >= 5 && hour < 12) {
                greeting = "GOOD MORNING, SIR ";
            } else if (hour >= 12 && hour < 18) {
                greeting = "GOOD AFTERNOON, SIR ";
            } else if (hour >= 18 && hour < 22) {
                greeting = "GOOD EVENING, SIR ";
            } else {
                greeting = "GOOD NIGHT, SIR ";
            }

            HI_USER_NAME.setText(greeting + username.toUpperCase());
        }
        if (balance_main != null) {
            // Tự động lắng nghe và định dạng số dư hiển thị dạng: 1,000,000.00
            balance_main.textProperty().bind(
                    ClientContext.getInstance().userBalanceProperty().asString("%,.0f")
            );
        }
        RequestSender.send(MessageType.GET_SHOP_PRODUCTS_REQUEST, null);
        RequestSender.send(MessageType.GET_BALANCE_REQUEST, null);
    }

    public void updateAvatar(String base64) {
        if (base64 != null && !base64.isEmpty() && main_avatar != null) {
            Platform.runLater(() -> {
                try {
                    byte[] imageBytes = Base64.getDecoder().decode(base64);
                    Image image = new Image(new ByteArrayInputStream(imageBytes));
                    main_avatar.setImage(image);
                } catch (Exception e) {
                    LOGGER.severe("[MainController] Lỗi load avatar: " + e.getMessage());
                }
            });
        }
    }


    public void updateProductCount(int count) {
        Platform.runLater(() -> {
            if (shop_number_product != null) {
                shop_number_product.setText(count+"");
            }
        });
    }


    @FXML
    public void handleBalanceClicked(MouseEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/bank.fxml");
    }

    @FXML
    public void handleAboutYouClicked(MouseEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/aboutYou.fxml");
    }

    @FXML
    public void handleBackMain(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/main.fxml");
    }

    @FXML
    public void handleShopClicked(MouseEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/Shop.fxml");
    }

    @FXML
    public void handleWonAuctionsClicked(MouseEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/wonAuctions.fxml");
    }
}