package com.auction.server.dao;

import com.auction.common.model.user.User;
import com.auction.server.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class UserDaoTest {

    private UserDao userDao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        userDao = UserDao.getInstance();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void testInsertBidderHashesPassword() throws SQLException {
        try (MockedStatic<Db> mockedDb = mockStatic(Db.class)) {
            mockedDb.when(Db::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            
            // Mock cho getUserByEmail (được gọi bên trong insertBidder)
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false); // Email chưa tồn tại

            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            String rawPassword = "mySecretPassword123";
            double balance = 500.0;
            userDao.insertBidder("test@email.com", rawPassword, "Test User", "U001", Timestamp.valueOf(LocalDateTime.now()), balance);

            // Verify that the password sent to PreparedStatement is NOT the raw password (index 3)
            verify(mockPreparedStatement).setString(eq(3), argThat(hashed -> 
                BCrypt.checkpw(rawPassword, hashed)
            ));

            // Verify that the balance is set (index 6)
            verify(mockPreparedStatement).setDouble(eq(6), eq(balance));
        }
    }

    @Test
    void testAuthenticateSuccess() throws SQLException {
        try (MockedStatic<Db> mockedDb = mockStatic(Db.class)) {
            mockedDb.when(Db::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            String email = "user@test.com";
            String rawPassword = "password123";
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getString("password")).thenReturn(hashedPassword);
            when(mockResultSet.getString("role")).thenReturn("BIDDER");
            when(mockResultSet.getString("id")).thenReturn("U001");
            when(mockResultSet.getString("email")).thenReturn(email);
            when(mockResultSet.getString("name")).thenReturn("Test User");

            User result = userDao.authenticate(email, rawPassword);

            assertNotNull(result);
            assertEquals(email, result.getEmail());
            verify(mockResultSet, times(1)).getString("password");
        }
    }

    @Test
    void testAuthenticateFailureWithWrongPassword() throws SQLException {
        try (MockedStatic<Db> mockedDb = mockStatic(Db.class)) {
            mockedDb.when(Db::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            String email = "user@test.com";
            String rawPassword = "correctPassword";
            String wrongPassword = "wrongPassword";
            String hashedPassword = BCrypt.hashpw(rawPassword, BCrypt.gensalt());

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getString("password")).thenReturn(hashedPassword);

            User result = userDao.authenticate(email, wrongPassword);

            assertNull(result);
        }
    }
}