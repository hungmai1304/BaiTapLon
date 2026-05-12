package com.auction.client.controller;

import com.auction.client.network.RequestSender;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.util.List;

public class ShopSellController {
    String email = SomeGlobal.getCurrentUser().getEmail();

    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colSTT;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colDesc;
    @FXML private TableColumn<Product, ProductStatus> colStatus;
    @FXML private TableColumn<Product, Void> colAction;
    @FXML private Label totalLabel;

    private ObservableList<Product> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {

        // 1. Đăng ký controller để Handler gọi được
        SomeGlobal.setShopSellController(this);

        // 2. Cột STT
        colSTT.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        // 3. Cột Tên
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        // 4. Cột Giá
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setText(empty || item == null ? null : String.format("%,.0fđ", item));
            }
        });
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));

        // 5. Cột Mô tả
        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // 6. Cột Trạng thái
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ProductStatus item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                switch (item) {
                    case AVAILABLE  -> { setText("Ở trong kho");   setStyle("-fx-text-fill: #f0a500;"); }
                    case ON_AUCTION -> { setText("Đang treo bán"); setStyle("-fx-text-fill: #6c63ff;"); }
                    case SOLD       -> { setText("Đã bán");        setStyle("-fx-text-fill: #4caf50;"); }
                    default         -> { setText(item.toString()); setStyle(""); }
                }
            }
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // 7. Cột Thao tác
        colAction.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit = new Button("Edit");
            private final Button btnSell = new Button("Sell");
            private final HBox box = new HBox(10, btnEdit, btnSell);
            {
                box.setAlignment(javafx.geometry.Pos.CENTER);
                btnEdit.setStyle("-fx-background-color: #f0a500; -fx-text-fill: white; -fx-cursor: hand;");
                btnSell.setStyle("-fx-background-color: #6c63ff; -fx-text-fill: white; -fx-cursor: hand;");
                btnEdit.setOnAction(e -> handleEdit(getTableView().getItems().get(getIndex())));
                btnSell.setOnAction(e -> handleSell(getTableView().getItems().get(getIndex())));
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        productTable.setItems(productList);

        // 8. Gửi request lấy danh sách từ server
        String email = SomeGlobal.getCurrentUser().getEmail();
        RequestSender.sendGetShopProductsRequest(email);
    }

    // Được gọi từ GetShopProductsClientHandler khi server trả về
    public void loadProducts(List<Product> products) {
        Platform.runLater(() -> {
            productList.setAll(products);
            totalLabel.setText("Tổng cộng: " + products.size() + " sản phẩm đang chờ");
        });
    }

    private void handleEdit(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(
                    "/com/auction/client/view/ShopImport.fxml"));
            VBox shopImportView = loader.load();
            ShopImportController controller = loader.getController();
            controller.fillProductData(product);
            HomeController homeController = SomeGlobal.getHomeController();
            if (homeController != null && homeController.getBorderpaneHome() != null) {
                homeController.getBorderpaneHome().setCenter(shopImportView);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleSell(Product product) {
        System.out.println("Sell: " + product.getName());
        // TODO: RequestSender.sendSellProductRequest(product.getId());
    }

    @FXML
    public void handleBackClicked(ActionEvent event) throws IOException {
        VBox shop_view = FXMLLoader.load(getClass().getResource(
                "/com/auction/client/view/Shop.fxml"));
        HomeController homeController = SomeGlobal.getHomeController();
        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(shop_view);
        }
    }
}