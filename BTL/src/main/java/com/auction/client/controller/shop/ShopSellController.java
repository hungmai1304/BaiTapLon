package com.auction.client.controller.shop;

import com.auction.client.controller.general.SomeGlobal;
import com.auction.client.controller.mainHome.HomeController;
import com.auction.client.network.RequestSender;
import com.auction.client.utils.ClientContext;
import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import javafx.application.Platform;
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
    @FXML private TableView<Product> productTable;
    @FXML private TableColumn<Product, Integer> colSTT;
    @FXML private TableColumn<Product, String> colName;
    @FXML private TableColumn<Product, Double> colPrice;
    @FXML private TableColumn<Product, String> colDesc;
    @FXML private TableColumn<Product, ProductStatus> colStatus;
    @FXML private TableColumn<Product, Void> colAction;
    @FXML private Label totalLabel;

    @FXML
    public void initialize() {
        SomeGlobal.setShopSellController(this);

        // Đổ dữ liệu từ Context vào Table ngay khi mở màn hình
        productTable.setItems(ClientContext.getInstance().getShopProducts());
        updateTotalLabel();

        // Cấu hình các cột hiển thị số thứ tự (STT)
        colSTT.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Integer item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        colName.setCellValueFactory(new PropertyValueFactory<>("name"));

        // Định dạng cột Giá tiền
        colPrice.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                setText(empty || item == null ? null : String.format("%,.0f", item));
            }
        });
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startPrice"));

        colDesc.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Cấu hình hiển thị màu sắc theo trạng thái sản phẩm
        colStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(ProductStatus item, boolean empty) {
                super.updateItem(item, empty);
                setAlignment(javafx.geometry.Pos.CENTER);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                switch (item) {
                    case AVAILABLE     -> { setText("Trong kho");     setStyle("-fx-text-fill: #f0a500;"); }
                    case ON_AUCTION    -> { setText("Đang treo bán"); setStyle("-fx-text-fill: #6c63ff;"); }
                    case NOT_AVAILABLE -> { setText("Đã bị cấm");    setStyle("-fx-text-fill: #e63946;"); } // Đổi sang màu đỏ cảnh báo cho rõ ràng
                    case SOLD          -> { setText("Đã bán");        setStyle("-fx-text-fill: #4caf50;"); }
                    default            -> { setText(item.toString()); setStyle(""); }
                }
            }
        });
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        // Cấu hình các nút chức năng (Hành động) trong TableView
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
                if (empty) {
                    setGraphic(null);
                } else {
                    // Lấy đối tượng Product tại dòng hiện tại
                    Product currentProduct = getTableView().getItems().get(getIndex());

                    if (currentProduct != null && currentProduct.getStatus() == ProductStatus.NOT_AVAILABLE) {
                        // Nếu sản phẩm BỊ CẤM (NOT_AVAILABLE) -> Loại bỏ hoàn toàn nút Sell khỏi layout HBox
                        box.getChildren().setAll(btnEdit);
                    } else {
                        // Nếu sản phẩm ở trạng thái bình thường khác -> Hiện đầy đủ cả 2 nút
                        box.getChildren().setAll(btnEdit, btnSell);
                    }
                    setGraphic(box);
                }
            }
        });
    }

    /**
     * Cập nhật lại Label tổng số lượng
     */
    private void updateTotalLabel() {
        int count = ClientContext.getInstance().getShopProductCount();
        totalLabel.setText("Tổng cộng: " + count + " sản phẩm đang chờ");
    }

    /**
     * Hàm này được gọi từ Handler khi có dữ liệu mới cập nhật đồng bộ từ Server
     */
    public void loadProducts(List<Product> products) {
        Platform.runLater(() -> {
            // Vì Table đã được bind trực tiếp với ObservableList trong Context nên chỉ cần update Label và làm mới giao diện
            updateTotalLabel();
            productTable.refresh();
        });
    }

    private void handleEdit(Product product) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/auction/client/view/editProduct.fxml"));
            VBox shopImportView = loader.load();
            editProductController controller = loader.getController();
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
        System.out.println("Sell (Gửi ID lên Server): " + product.getId());
        RequestSender.sendSellProductRequest(String.valueOf(product.getId()));
    }

    @FXML
    public void handleBackClicked(ActionEvent event) throws IOException {
        VBox shop_view = FXMLLoader.load(getClass().getResource("/com/auction/client/view/Shop.fxml"));
        HomeController homeController = SomeGlobal.getHomeController();
        if (homeController != null && homeController.getBorderpaneHome() != null) {
            homeController.getBorderpaneHome().setCenter(shop_view);
        }
    }
}