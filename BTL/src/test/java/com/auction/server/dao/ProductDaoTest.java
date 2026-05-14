package com.auction.server.dao;

import com.auction.common.model.product.Product;
import com.auction.common.model.product.ProductStatus;
import org.junit.jupiter.api.Test;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductDaoTest {

    @Test
    void testMapResultSetToProduct() throws Exception {
        ProductDao dao = ProductDao.getInstance();
        ResultSet rs = mock(ResultSet.class);
        
        LocalDateTime now = LocalDateTime.now();
        Timestamp timestamp = Timestamp.valueOf(now);

        when(rs.getString("id")).thenReturn("P001");
        when(rs.getString("name")).thenReturn("Test Product");
        when(rs.getString("category")).thenReturn("Electronics");
        when(rs.getString("description")).thenReturn("Desc");
        when(rs.getString("image_path")).thenReturn("http://image.url");
        when(rs.getDouble("start_price")).thenReturn(100.0);
        when(rs.getDouble("current_price")).thenReturn(150.0);
        when(rs.getDouble("step_price")).thenReturn(10.0);
        when(rs.getString("status")).thenReturn("AVAILABLE");
        when(rs.getTimestamp("time_created")).thenReturn(timestamp);
        when(rs.getString("owner_id")).thenReturn("U001");

        // Use reflection to call the private method mapResultSetToProduct
        Method method = ProductDao.class.getDeclaredMethod("mapResultSetToProduct", ResultSet.class);
        method.setAccessible(true);
        Product product = (Product) method.invoke(dao, rs);

        assertNotNull(product);
        assertEquals("P001", product.getId());
        assertEquals("Test Product", product.getName());
        assertEquals(ProductStatus.AVAILABLE, product.getStatus());
        assertEquals(150.0, product.getCurrentPrice());
        assertEquals("U001", product.getOwner().getId());
    }
}
