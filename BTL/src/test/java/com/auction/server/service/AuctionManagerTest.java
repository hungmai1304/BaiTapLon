package com.auction.server.service;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.server.AuctionWebSocketServer;
import com.auction.server.model.ServerContext;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuctionManagerTest {

    private AuctionManager auctionManager;
    private ServerContext serverContext;
    private AuctionWebSocketServer mockServer;

    @BeforeEach
    void setUp() {
        auctionManager = AuctionManager.getInstance();
        serverContext = ServerContext.getInstance();
        mockServer = mock(AuctionWebSocketServer.class);

        // Clear existing products in singleton context
        serverContext.getProductList().clear();
        serverContext.setCurrentProduct(null);
        
        // Mock the server in context
        serverContext.initData(mockServer, null);
        
        // Mock connections to avoid NPE during broadcast
        when(mockServer.getConnections()).thenReturn(new HashSet<>());
    }

    @Test
    void testPickNextProduct_NoAvailableProducts() {
        Product picked = auctionManager.pickNextProduct();
        assertNull(picked);
    }

    @Test
    void testPickNextProduct_Success() {
        Product p1 = new Product();
        p1.setId("1");
        p1.setName("Test Product");
        p1.setStatus(ProductStatus.AVAILABLE);
        serverContext.addProduct(p1);

        Product picked = auctionManager.pickNextProduct();

        assertNotNull(picked);
        assertEquals("1", picked.getId());
        assertEquals(ProductStatus.ON_AUCTION, picked.getStatus());
        assertEquals(picked, serverContext.getCurrentProduct());
        assertNotNull(picked.getStartTime());
        assertNotNull(picked.getEndTime());
        
        verify(mockServer, atLeastOnce()).getConnections();
    }

    @Test
    void testEndAuction_Sold() {
        Product p1 = new Product();
        p1.setId("1");
        p1.setName("Test Product");
        p1.setStartPrice(100);
        p1.setCurrentPrice(150); // Price increased
        p1.setStatus(ProductStatus.ON_AUCTION);

        auctionManager.endAuction(p1);

        assertEquals(ProductStatus.SOLD, p1.getStatus());
        verify(mockServer, atLeastOnce()).getConnections();
    }

    @Test
    void testEndAuction_Cancelled() {
        Product p1 = new Product();
        p1.setId("1");
        p1.setName("Test Product");
        p1.setStartPrice(100);
        p1.setCurrentPrice(100); // No price increase
        p1.setStatus(ProductStatus.ON_AUCTION);

        auctionManager.endAuction(p1);

        assertEquals(ProductStatus.CANCELLED, p1.getStatus());
        verify(mockServer, atLeastOnce()).getConnections();
    }
}
