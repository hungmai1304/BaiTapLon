package com.auction.server.model;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import com.auction.common.model.user.User;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ServerContextTest {

    private ServerContext serverContext;

    @BeforeEach
    void setUp() {
        serverContext = ServerContext.getInstance();
        // Since it's a singleton, we might need to clear state if possible, 
        // but looking at ServerContext, it doesn't have a clear method.
        // We will just test with fresh objects.
    }

    @Test
    @DisplayName("Test Singleton pattern")
    void testSingleton() {
        ServerContext instance2 = ServerContext.getInstance();
        assertSame(serverContext, instance2, "ServerContext should be a singleton");
    }

    @Test
    @DisplayName("Test User management")
    void testUserManagement() {
        WebSocket mockConn = mock(WebSocket.class);
        String userId = "test@user.com";

        serverContext.addOnlineUser(userId, mockConn);
        assertEquals(userId, serverContext.getUserByConn(mockConn));

        serverContext.removeUser(mockConn);
        assertNull(serverContext.getUserByConn(mockConn));
    }

    @Test
    @DisplayName("Test User Object caching")
    void testUserObjectCaching() {
        WebSocket mockConn = mock(WebSocket.class);
        User mockUser = new User();
        mockUser.setEmail("test@user.com");
        mockUser.setUsername("testuser");

        serverContext.addOnlineUserObject(mockConn, mockUser);
        assertEquals(mockUser, serverContext.getUserCacheByConn(mockConn));

        serverContext.removeOnlineUserObject(mockConn);
        assertNull(serverContext.getUserCacheByConn(mockConn));
    }

    @Test
    @DisplayName("Test Auction management")
    void testAuctionManagement() {
        Product product = new Product();
        product.setId("prod123");
        
        Auction auction = new Auction();
        auction.setId("auc123");
        auction.setProduct(product);

        serverContext.addAuction(auction);
        assertEquals(auction, serverContext.getAuctionByProductId("prod123"));

        serverContext.removeAuction("auc123");
        assertNull(serverContext.getAuctionByProductId("prod123"));
    }

    @Test
    @DisplayName("Test TikTok Listener management")
    void testTikTokListeners() {
        WebSocket mockConn = mock(WebSocket.class);

        serverContext.addTikTokListener(mockConn);
        assertTrue(serverContext.getTikTokListeners().contains(mockConn));

        serverContext.removeTikTokListener(mockConn);
        assertFalse(serverContext.getTikTokListeners().contains(mockConn));
    }
}
