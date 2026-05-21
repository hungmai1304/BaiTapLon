package com.auction.server.dao;

import com.auction.server.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuctionDaoTest {

    private AuctionDao auctionDao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;

    @BeforeEach
    void setUp() throws SQLException {
        auctionDao = AuctionDao.getInstance();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
    }

    //Lưu lịch sử đấu giá(nếu thành công)

    @Test
    void testSaveCompletedAuction() throws SQLException {
        try (MockedStatic<Db> mockedDb = mockStatic(Db.class)) {
            mockedDb.when(Db::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            boolean success = auctionDao.saveCompletedAuction("A001", "P001", "winner@test.com", 200.0);

            assertTrue(success);
            verify(mockPreparedStatement).setString(1, "A001");
            verify(mockPreparedStatement).setString(2, "P001");
            verify(mockPreparedStatement).setString(3, "winner@test.com");
            verify(mockPreparedStatement).setDouble(4, 200.0);
        }
    }
}
