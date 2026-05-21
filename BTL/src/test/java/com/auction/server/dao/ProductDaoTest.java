package com.auction.server.dao;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import com.auction.server.db.Db;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.lang.reflect.Method;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ProductDaoTest {

    private ProductDao productDao;
    private Connection mockConnection;
    private PreparedStatement mockPreparedStatement;
    private Statement mockStatement;
    private ResultSet mockResultSet;

    @BeforeEach
    void setUp() throws SQLException {
        productDao = ProductDao.getInstance();
        mockConnection = mock(Connection.class);
        mockPreparedStatement = mock(PreparedStatement.class);
        mockStatement = mock(Statement.class);
        mockResultSet = mock(ResultSet.class);
    }

    @Test
    void testMapResultSetToProduct() throws Exception {
        LocalDateTime now = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(now);

        when(mockResultSet.getString("id")).thenReturn("P001");
        when(mockResultSet.getString("name")).thenReturn("Test Product");
        when(mockResultSet.getString("category")).thenReturn("Electronics");
        when(mockResultSet.getString("status")).thenReturn("AVAILABLE");
        when(mockResultSet.getTimestamp("time_created")).thenReturn(timestamp);
        when(mockResultSet.getString("owner_id")).thenReturn("U001");

        // Use reflection to call the private method mapResultSetToProduct
        Method method = ProductDao.class.getDeclaredMethod("mapResultSetToProduct", ResultSet.class);
        method.setAccessible(true);
        Product product = (Product) method.invoke(productDao, mockResultSet);

        assertNotNull(product);
        assertEquals("P001", product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals(ProductStatus.AVAILABLE, product.getStatus());
    }

    @Test
    void testGetAllProducts() throws SQLException {
        try (MockedStatic<Db> mockedDb = mockStatic(Db.class)) {
            mockedDb.when(Db::getConnection).thenReturn(mockConnection);
            when(mockConnection.createStatement()).thenReturn(mockStatement);
            when(mockStatement.executeQuery(anyString())).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true, false);
            when(mockResultSet.getString("id")).thenReturn("P001");
            when(mockResultSet.getString("name")).thenReturn("Product 1");
            when(mockResultSet.getString("status")).thenReturn("AVAILABLE");

            List<Product> products = productDao.getAllProducts();

            assertEquals(1, products.size());
            assertEquals("P001", products.get(0).getId());
        }
    }

    @Test
    void testSaveProduct() throws SQLException {
        try (MockedStatic<Db> mockedDb = mockStatic(Db.class)) {
            mockedDb.when(Db::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            Product p = new Product();
            p.setId("P002");
            p.setName("New Product");
            p.setStatus(ProductStatus.AVAILABLE);

            boolean success = productDao.saveProduct(p);

            assertTrue(success);
            verify(mockPreparedStatement).setString(1, "P002");
            verify(mockPreparedStatement).setString(2, "New Product");
        }
    }

    @Test
    void testGetProductById() throws SQLException {
        try (MockedStatic<Db> mockedDb = mockStatic(Db.class)) {
            mockedDb.when(Db::getConnection).thenReturn(mockConnection);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);

            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getString("id")).thenReturn("P001");
            when(mockResultSet.getString("status")).thenReturn("AVAILABLE");

            Product p = productDao.getProductById("P001");

            assertNotNull(p);
            assertEquals("P001", p.getId());
        }
    }
}

