package com.auction.server.model;

import com.auction.server.AuctionWebSocketServer;
import com.auction.common.model.product.Product;

public class ServerContext {
    private final AuctionWebSocketServer server;
    private final Product currentProduct;

    public ServerContext(AuctionWebSocketServer server, Product currentProduct) {
        this.server = server;
        this.currentProduct = currentProduct;
    }

    public AuctionWebSocketServer getServer() { return server; }
    public Product getCurrentProduct() { return currentProduct; }
}