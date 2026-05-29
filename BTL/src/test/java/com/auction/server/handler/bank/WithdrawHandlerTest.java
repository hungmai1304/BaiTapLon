package com.auction.server.handler.bank;

import com.auction.common.model.user.User;
import com.auction.server.dao.UserDao;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class WithdrawHandlerTest {

    private WithdrawHandler handler;
    private WebSocket mockConn;
    private Gson gson;
    private ServerContext mockContext;
    private UserDao mockUserDao;
    private MockedStatic<UserDao> mockedUserDaoStatic;

    @BeforeEach
    void setUp() {
        handler = new WithdrawHandler();
        mockConn = mock(WebSocket.class);
        gson = new Gson();
        mockContext = mock(ServerContext.class);
        mockUserDao = mock(UserDao.class);

        mockedUserDaoStatic = mockStatic(UserDao.class);
        mockedUserDaoStatic.when(UserDao::getInstance).thenReturn(mockUserDao);
    }

    @AfterEach
    void tearDown() {
        mockedUserDaoStatic.close();
    }

    @Test
    void testNotLoggedIn() {
        Map<String, Object> data = new HashMap<>();
        when(mockContext.getUserByConn(mockConn)).thenReturn(null);
        handler.handle(mockConn, data, gson, mockContext);
        verify(mockConn).send(contains("Bạn cần đăng nhập"));
    }

    @Test
    void testInvalidAmount() {
        Map<String, Object> data = new HashMap<>();
        data.put("data", -50.0);
        when(mockContext.getUserByConn(mockConn)).thenReturn("user@test.com");
        handler.handle(mockConn, data, gson, mockContext);
        verify(mockConn).send(contains("Số tiền rút phải lớn hơn 0"));
    }

    @Test
    void testWithdrawSuccess() {
        Map<String, Object> data = new HashMap<>();
        data.put("data", 500.0);
        String email = "user@test.com";

        when(mockContext.getUserByConn(mockConn)).thenReturn(email);
        when(mockUserDao.withdrawMoney(email, 500.0)).thenReturn(true);
        
        User updatedUser = new User();
        updatedUser.setBalance(1000.0); // Assuming previous balance was 1500
        when(mockUserDao.getUserByEmail(email)).thenReturn(updatedUser);

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("SUCCESS"));
        verify(mockConn).send(contains("1000"));
    }
    
    @Test
    void testWithdrawFailInsufficientFunds() {
        Map<String, Object> data = new HashMap<>();
        data.put("data", 50000.0);
        String email = "user@test.com";

        when(mockContext.getUserByConn(mockConn)).thenReturn(email);
        when(mockUserDao.withdrawMoney(email, 50000.0)).thenReturn(false); // DAO returns false

        handler.handle(mockConn, data, gson, mockContext);

        verify(mockConn).send(contains("ERROR"));
        verify(mockConn).send(contains("Rút tiền thất bại"));
    }
}
