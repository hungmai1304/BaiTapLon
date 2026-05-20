package com.auction.client.controller.mainHome;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.client.utils.ClientContext;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.MessageType;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

public class SearchController implements Initializable {

    @FXML private TextField tfSearchKeyword;
    @FXML private FlowPane fpSearchResults;
    @FXML private Label lblResultCount;

    private String currentCategory = null;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        ControllerRegistry.register("SearchController", this);
        
        // Load all auctions initially
        performSearch(null, null);
    }

    @FXML
    public void handleSearchAction() {
        String keyword = tfSearchKeyword.getText();
        performSearch(keyword, currentCategory);
    }

    @FXML
    public void handleCategoryArt() {
        currentCategory = "Art";
        performSearch(tfSearchKeyword.getText(), currentCategory);
    }

    @FXML
    public void handleCategoryElectronics() {
        currentCategory = "Electronics";
        performSearch(tfSearchKeyword.getText(), currentCategory);
    }

    @FXML
    public void handleCategoryFashion() {
        currentCategory = "Fashion";
        performSearch(tfSearchKeyword.getText(), currentCategory);
    }

    @FXML
    public void handleCategoryVehicles() {
        currentCategory = "Vehicles";
        performSearch(tfSearchKeyword.getText(), currentCategory);
    }

    @FXML
    public void handleCategoryProperty() {
        currentCategory = "Property";
        performSearch(tfSearchKeyword.getText(), currentCategory);
    }

    @FXML
    public void handleCategoryAll() {
        currentCategory = null;
        performSearch(tfSearchKeyword.getText(), null);
    }

    private void performSearch(String keyword, String category) {
        Map<String, Object> filters = new HashMap<>();
        if (keyword != null && !keyword.isBlank()) filters.put("keyword", keyword);
        if (category != null && !category.isBlank()) filters.put("category", category);
        
        RequestSender.send(MessageType.GET_ACTIVE_AUCTIONS_REQUEST, filters);
        
        Platform.runLater(() -> {
            lblResultCount.setText("Searching...");
            fpSearchResults.getChildren().clear();
        });
    }

    /**
     * Called by GetActiveAuctionsClientHandler via Reflection
     */
    public void renderResults(List<Auction> results) {
        Platform.runLater(() -> {
            fpSearchResults.getChildren().clear();
            if (results == null || results.isEmpty()) {
                lblResultCount.setText("No auctions found matching your criteria.");
                return;
            }

            lblResultCount.setText("Found " + results.size() + " active auctions");

            for (Auction auction : results) {
                VBox card = createProductCard(auction);
                fpSearchResults.getChildren().add(card);
            }
        });
    }

    private VBox createProductCard(Auction auction) {
        VBox card = new VBox(10);
        card.setPrefSize(280, 380);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-background-radius: 15; " +
                     "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5); " +
                     "-fx-cursor: hand;");
        card.setPadding(new Insets(15));
        card.setAlignment(Pos.TOP_CENTER);

        // Image Placeholder (or real image if available)
        StackPane imgContainer = new StackPane();
        imgContainer.setPrefHeight(180);
        imgContainer.setStyle("-fx-background-color: #F1F8FE; -fx-background-radius: 10;");
        
        ImageView imageView = new ImageView();
        try {
            // Check if product has image, else use default
            String imgPath = auction.getProduct() != null ? auction.getProduct().getImagePath() : null;
            if (imgPath != null && !imgPath.isEmpty()) {
                imageView.setImage(new Image(imgPath, true));
            } else {
                imageView.setImage(new Image(getClass().getResource("/com/auction/client/view/products.png").toExternalForm()));
            }
        } catch (Exception e) {
            // Fallback
        }
        imageView.setFitHeight(140);
        imageView.setFitWidth(140);
        imageView.setPreserveRatio(true);
        imgContainer.getChildren().add(imageView);

        // Product Name
        String productName = auction.getProduct() != null ? auction.getProduct().getName() : "Unknown Product";
        Label lblName = new Label(productName);
        lblName.setFont(Font.font("System", FontWeight.BOLD, 18));
        lblName.setWrapText(true);
        lblName.setMaxWidth(250);
        lblName.setAlignment(Pos.CENTER);

        // Category Badge
        String category = auction.getProduct() != null ? auction.getProduct().getCategory() : "General";
        Label lblCat = new Label(category);
        lblCat.setStyle("-fx-background-color: #E3F2FD; -fx-text-fill: #1976D2; -fx-padding: 2 10; -fx-background-radius: 10; -fx-font-size: 12;");

        // Price
        Label lblPrice = new Label(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        lblPrice.setFont(Font.font("System", FontWeight.EXTRA_BOLD, 22));
        lblPrice.setTextFill(Color.web("#2E7D32"));

        // Seller Info
        Label lblSeller = new Label("Seller: " + (auction.getHighestBidder() != null ? auction.getHighestBidder().getUsername() : "Original Owner"));
        lblSeller.setFont(Font.font("System", 14));
        lblSeller.setTextFill(Color.GRAY);

        card.getChildren().addAll(imgContainer, lblCat, lblName, lblPrice, lblSeller);

        // Interaction
        card.setOnMouseClicked(e -> {
            // Set current auction in context so BiddingBoard knows what to show
            ClientContext.getInstance().setCurrentAuctionByObject(auction);
            NavigationService.setCenterView("/com/auction/client/view/biddingBoard.fxml");
        });

        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle(card.getStyle() + "-fx-background-color: #FAFDFF; -fx-translate-y: -5;"));
        card.setOnMouseExited(e -> card.setStyle(card.getStyle().replace("-fx-background-color: #FAFDFF; -fx-translate-y: -5;", "")));

        return card;
    }
}