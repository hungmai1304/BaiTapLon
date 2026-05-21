package com.auction.client.controller.mainHome;

import com.auction.client.network.RequestSender;
import com.auction.client.utils.ControllerRegistry;
import com.auction.client.utils.NavigationService;
import com.auction.common.model.auction.Auction;
import com.auction.protocol.MessageType;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class WonAuctionsController {

    @FXML private TableView<Auction> wonAuctionsTable;
    @FXML private TableColumn<Auction, String> colProductName;
    @FXML private TableColumn<Auction, String> colProductImage;
    @FXML private TableColumn<Auction, String> colSeller;
    @FXML private TableColumn<Auction, Double> colFinalPrice;
    @FXML private TableColumn<Auction, String> colTime;

    @FXML
    public void initialize() {
        ControllerRegistry.register("WonAuctionsController", this);

        // Setup columns
        colProductName.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProduct().getName()));
            
        colSeller.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProduct().getOwner().getUsername()));
            
        colFinalPrice.setCellValueFactory(cellData -> 
            new SimpleObjectProperty<>(cellData.getValue().getCurrentPrice()));
            
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        colTime.setCellValueFactory(cellData -> {
            LocalDateTime endTime = cellData.getValue().getEndTime();
            if (endTime != null) {
                // Thêm 7 tiếng để đúng múi giờ Việt Nam (giống BiddingController)
                return new SimpleStringProperty(endTime.plusHours(7).format(formatter));
            }
            return new SimpleStringProperty("N/A");
        });

        colProductImage.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getProduct().getImagePath()));

        colProductImage.setCellFactory(param -> new TableCell<Auction, String>() {
            private final ImageView imageView = new ImageView();
            @Override
            protected void updateItem(String path, boolean empty) {
                super.updateItem(path, empty);
                if (empty || path == null || path.isEmpty()) {
                    setGraphic(null);
                } else {
                    try {
                        // Load image with background loading enabled
                        Image img = new Image(path, 50, 50, true, true);
                        imageView.setImage(img);
                        setGraphic(imageView);
                    } catch (Exception e) {
                        setGraphic(null);
                    }
                }
            }
        });

        // Gửi yêu cầu lấy danh sách sản phẩm thắng
        RequestSender.send(MessageType.GET_WON_AUCTIONS_REQUEST, null);
    }

    public void updateTable(List<Auction> wonAuctions) {
        wonAuctionsTable.setItems(FXCollections.observableArrayList(wonAuctions));
    }

    @FXML
    public void handleBack(ActionEvent event) {
        NavigationService.setCenterView("/com/auction/client/view/main.fxml");
    }
}
