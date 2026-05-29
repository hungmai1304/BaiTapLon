package com.auction.server.handler.bidding;

import com.auction.common.model.auction.Auction;
import com.auction.common.model.product.Product;
import com.auction.common.model.user.User;
import com.auction.protocol.Response;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class PlaceBidHandlerTest {

    private PlaceBidHandler handler;
    private WebSocket mockConn;
    private Gson gson;
    private ServerContext mockContext;
    private UserDao mockUserDao;
    private MockedStatic<UserDao> mockedUserDaoStatic;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        handler = new PlaceBidHandler();
        mockConn = mock(WebSocket.class);
        gson = new Gson();
        mockContext = mock(ServerContext.class);
        mockUserDao = mock(UserDao.class);
        
        mockedUserDaoStatic = mockStatic(UserDao.class);
        mockedUserDaoStatic.when(UserDao::getInstance).thenReturn(mockUserDao);

        // Reset userCooldowns via reflection to avoid state leaking between tests
        Field cooldownsField = PlaceBidHandler.class.getDeclaredField("userCooldowns");
        cooldownsField.setAccessible(true);
        Map<String, Long> cooldowns = (Map<String, Long>) cooldownsField.get(null);
        cooldowns.clear();
    }

    @AfterEach
    void tearDown() {
        mockedUserDaoStatic.close();
    }

    @Test
    void testNotLoggedIn() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", "P001");
        data.put("bidAmount", 100.0);

        when(mockContext.getUserByConn(mockConn)).thenReturn(null);

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("Bạn chưa đăng nhập hoặc phiên làm việc hết hạn!"));
    }

    @Test
    void testMissingProductId() {
        Map<String, Object> data = new HashMap<>();
        data.put("bidAmount", 100.0);

        when(mockContext.getUserByConn(mockConn)).thenReturn("user@test.com");

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("Thiếu ID sản phẩm!"));
    }

    @Test
    void testUserBlacklisted() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", "P001");
        data.put("bidAmount", 100.0);

        String email = "user@test.com";
        when(mockContext.getUserByConn(mockConn)).thenReturn(email);

        User user = new User();
        user.setEmail(email);
        user.setStatus("BLACKLIST");
        when(mockUserDao.getUserByEmail(email)).thenReturn(user);

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("Tài khoản của bạn đã bị khóa tham gia đấu giá"));
    }

    @Test
    void testAuctionNotFound() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", "P001");
        data.put("bidAmount", 100.0);

        String email = "user@test.com";
        when(mockContext.getUserByConn(mockConn)).thenReturn(email);

        User user = new User();
        user.setEmail(email);
        user.setStatus("ACTIVE");
        when(mockUserDao.getUserByEmail(email)).thenReturn(user);

        when(mockContext.getAuctionByProductId("P001")).thenReturn(null);

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("Sản phẩm hiện không nằm trong phiên đấu giá nào"));
    }

    @Test
    void testSellerCannotBidOnOwnProduct() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", "P001");
        data.put("bidAmount", 100.0);

        String email = "seller@test.com";
        when(mockContext.getUserByConn(mockConn)).thenReturn(email);

        User seller = new User();
        seller.setId("U001");
        seller.setEmail(email);
        seller.setStatus("ACTIVE");
        when(mockUserDao.getUserByEmail(email)).thenReturn(seller);

        Auction auction = new Auction();
        Product product = new Product();
        product.setOwner(seller);
        auction.setProduct(product);
        when(mockContext.getAuctionByProductId("P001")).thenReturn(auction);

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("Bạn là người bán sản phẩm này, không được phép tự đặt giá!"));
    }

    @Test
    void testBidAmountTooLow() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", "P001");
        data.put("bidAmount", 100.0);

        String email = "bidder@test.com";
        when(mockContext.getUserByConn(mockConn)).thenReturn(email);

        User bidder = new User();
        bidder.setId("U002");
        bidder.setEmail(email);
        bidder.setStatus("ACTIVE");
        when(mockUserDao.getUserByEmail(email)).thenReturn(bidder);

        Auction auction = new Auction();
        auction.setStatus("ACTIVE");
        auction.setStartPrice(50);
        auction.setCurrentPrice(150); // Current price is higher than bid amount
        auction.setStepPrice(10);
        
        User currentHighest = new User();
        currentHighest.setEmail("other@test.com");
        auction.setHighestBidder(currentHighest);
        
        Product product = new Product();
        User owner = new User();
        owner.setEmail("seller@test.com");
        product.setOwner(owner);
        auction.setProduct(product);
        
        when(mockContext.getAuctionByProductId("P001")).thenReturn(auction);

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("Giá đưa ra đã bị người khác dẫn trước"));
    }

    @Test
    void testInsufficientBalance() {
        Map<String, Object> data = new HashMap<>();
        data.put("productId", "P001");
        data.put("bidAmount", 200.0);

        String email = "bidder@test.com";
        when(mockContext.getUserByConn(mockConn)).thenReturn(email);

        User bidder = new User();
        bidder.setId("U002");
        bidder.setEmail(email);
        bidder.setStatus("ACTIVE");
        when(mockUserDao.getUserByEmail(email)).thenReturn(bidder);

        Auction auction = new Auction();
        auction.setStatus("ACTIVE");
        auction.setStartPrice(50);
        auction.setCurrentPrice(150); 
        auction.setStepPrice(10);
        
        User currentHighest = new User();
        currentHighest.setEmail("other@test.com");
        auction.setHighestBidder(currentHighest);
        
        Product product = new Product();
        User owner = new User();
        owner.setEmail("seller@test.com");
        product.setOwner(owner);
        auction.setProduct(product);
        
        when(mockContext.getAuctionByProductId("P001")).thenReturn(auction);
        when(mockUserDao.withdrawMoney(email, 200.0)).thenReturn(false); // Insufficient funds

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("Số dư ví không đủ"));
    }
}
