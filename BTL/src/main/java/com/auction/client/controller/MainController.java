package com.auction.client.controller;
import com.auction.server.service.AuctionServer;
import com.auction.server.service.AuctionServer;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;


import java.io.IOException;
import javafx.scene.input.MouseEvent;


public class MainController {
    @FXML
    private GridPane gidpane_main;
    @FXML
    private AnchorPane balance_anchorpane;

    @FXML
    public void handleBalanceClicked(MouseEvent event) throws IOException {
        //1.khi ấn vào , cho màn balance đang ẩn hiện lên
        balance_anchorpane.setVisible(true);
        balance_anchorpane.setManaged(true);
        // cho man grid pane an di
        gidpane_main.setVisible(false);
        gidpane_main.setManaged(false);
    }
    @FXML
    public void handleClickedBack(ActionEvent event){
        balance_anchorpane.setVisible(false);
        balance_anchorpane.setManaged(false);

        gidpane_main.setVisible(true);
        gidpane_main.setManaged(true);
    }
    @FXML
    private AnchorPane withdraw_anchor;
    @FXML
    private AnchorPane deposit_anchor;
    @FXML
    public void handleWithdrawClicked(ActionEvent event){
        // hien withdraw len, an deposit di
        withdraw_anchor.setVisible(true);
        withdraw_anchor.setManaged(true);
        deposit_anchor.setVisible(false);
        deposit_anchor.setManaged(false);
    }
    @FXML
    public void handleDepositeClicked(ActionEvent event){
        deposit_anchor.setVisible(true);
        deposit_anchor.setVisible(true);
        withdraw_anchor.setVisible(false);
        withdraw_anchor.setManaged(false);
    }
}