package com.auction.client.controller;

import com.auction.client.network.ClientMessageDispatcher;
import com.auction.client.network.MessageListener;
import com.auction.client.network.NetworkClient;

import com.auction.common.model.product.Product;
import com.auction.protocol.Response;

import com.google.gson.Gson;

import javafx.application.Platform;

import javafx.event.ActionEvent;

import javafx.fxml.FXML;

import javafx.scene.control.Label;

public class TikTokAuctionController {

    @FXML
    private Label name;

    @FXML
    private Label price;

    @FXML
    private Label step;

    private final Gson gson = new Gson();

    // =========================================================
    // INIT
    // =========================================================
    @FXML
    public void initialize() {

        MessageListener auctionListener =
                new MessageListener() {

                    @Override
                    public void onMessageReceived(
                            String message
                    ) {

                        Platform.runLater(() -> {

                            try {

                                Response response =
                                        gson.fromJson(
                                                message,
                                                Response.class
                                        );

                                // =====================================
                                // ERROR
                                // =====================================
                                if ("FAILED".equals(
                                        response.getStatus()
                                )) {

                                    System.out.println(
                                            "❌ Lỗi: "
                                                    + response.getMessage()
                                    );

                                    return;
                                }

                                // =====================================
                                // PRODUCT UPDATE
                                // =====================================
                                Product product =
                                        gson.fromJson(
                                                gson.toJson(
                                                        response.getData()
                                                ),
                                                Product.class
                                        );

                                setCurrentAuction(product);

                                System.out.println(
                                        "✅ Đã cập nhật UI:"
                                                + product.getName()
                                );

                            } catch (Exception e) {

                                System.err.println(
                                        "❌ Lỗi parse JSON"
                                );

                                e.printStackTrace();
                            }
                        });
                    }
                };

        ClientMessageDispatcher.register(
                "PRODUCT_RESPONSE",
                auctionListener
        );
    }

    // =========================================================
    // UPDATE UI
    // =========================================================
    public void setCurrentAuction(Product product) {

        if (product != null) {

            name.setText(
                    product.getName()
            );

            price.setText(
                    String.valueOf(
                            product.getCurrentPrice()
                    )
            );

            step.setText(
                    String.valueOf(
                            product.getStepPrice()
                    )
            );
        }
    }

    // =========================================================
    // PREVIOUS PRODUCT
    // =========================================================
    @FXML
    public void handleUp(ActionEvent event) {

        System.out.println(
                "⬆ Đang chuyển sản phẩm trước..."
        );

        NetworkClient.sendCommand(
                "GET_BACK"
        );
    }

    // =========================================================
    // NEXT PRODUCT
    // =========================================================
    @FXML
    public void handleDown(ActionEvent event) {

        System.out.println(
                "⬇ Đang chuyển sản phẩm tiếp..."
        );

        NetworkClient.sendCommand(
                "GET_NEXT"
        );
    }
}