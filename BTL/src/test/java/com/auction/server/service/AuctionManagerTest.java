package com.auction.server.service;

import com.auction.common.model.auction.Auction;
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

        // Clear existing auctions in singleton context
        serverContext.getActiveAuctions().clear();
        
        // Mock the server in context
        serverContext.initData(mockServer);
        
        // Mock connections to avoid NPE during broadcast
        when(mockServer.getConnections()).thenReturn(new HashSet<>());
    }

    @Test
    void testPickNextProduct_NoAvailableAuctions() {
        Auction picked = auctionManager.pickNextProduct();
        assertNull(picked);
    }

    @Test
    void testPickNextProduct_Success() {
        Product p1 = new Product();
        p1.setId("1");
        p1.setName("Test Product");
        p1.setStatus(ProductStatus.AVAILABLE);
        
        Auction a1 = new Auction();
        a1.setId("1");
        a1.setProduct(p1);
        a1.setStatus("PENDING");
        
        serverContext.addAuction(a1);

        Auction picked = auctionManager.pickNextProduct();

        assertNotNull(picked);
        assertEquals("1", picked.getId());
        assertEquals("ACTIVE", picked.getStatus());
        assertEquals(ProductStatus.ON_AUCTION, picked.getProduct().getStatus());
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
        p1.setStatus(ProductStatus.ON_AUCTION);
        
        com.auction.common.model.user.User bidder = new com.auction.common.model.user.User();
        bidder.setEmail("winner@test.com");
        bidder.setUsername("winner");

        Auction a1 = new Auction();
        a1.setId("1");
        a1.setProduct(p1);
        a1.setStartPrice(100);
        a1.setCurrentPrice(150); // Price increased
        a1.setStatus("ACTIVE");
        a1.setHighestBidder(bidder);

        auctionManager.endAuction(a1);

        assertEquals("COMPLETED", a1.getStatus());
        assertEquals(ProductStatus.SOLD, p1.getStatus());
        verify(mockServer, atLeastOnce()).getConnections();
    }

    @Test
    void testEndAuction_Cancelled() {
        Product p1 = new Product();
        p1.setId("1");
        p1.setName("Test Product");
        p1.setStartPrice(100);
        p1.setStatus(ProductStatus.ON_AUCTION);
        
        Auction a1 = new Auction();
        a1.setId("1");
        a1.setProduct(p1);
        a1.setStartPrice(100);
        a1.setCurrentPrice(100); // No price increase
        a1.setStatus("ACTIVE");
        // No highest bidder

        auctionManager.endAuction(a1);

        assertEquals("COMPLETED", a1.getStatus());
        assertEquals(ProductStatus.AVAILABLE, p1.getStatus());
        verify(mockServer, atLeastOnce()).getConnections();
    }
}
