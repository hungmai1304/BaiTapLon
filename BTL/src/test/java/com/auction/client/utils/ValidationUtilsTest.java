package com.auction.client.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    @DisplayName("Test isValidCredentials with various inputs")
    void testIsValidCredentials() {
        assertTrue(ValidationUtils.isValidCredentials("test@example.com", "password123"));
        assertFalse(ValidationUtils.isValidCredentials("invalid-email", "password123"), "Email missing @ should be invalid");
        assertFalse(ValidationUtils.isValidCredentials("test@example.com", "12345"), "Password less than 6 chars should be invalid");
        assertFalse(ValidationUtils.isValidCredentials(null, "password123"), "Null email should be invalid");
        assertFalse(ValidationUtils.isValidCredentials("test@example.com", null), "Null password should be invalid");
    }

    @Test
    @DisplayName("Test validateRegister with various inputs")
    void testValidateRegister() {
        // Valid case
        assertEquals("VALID", ValidationUtils.validateRegister("John Doe", "john@example.com", "password123", "password123", "YES"));

        // Invalid cases
        assertEquals("Tên không được để trống!", ValidationUtils.validateRegister("", "john@example.com", "password123", "password123", "YES"));
        assertEquals("Tên không được để trống!", ValidationUtils.validateRegister(null, "john@example.com", "password123", "password123", "YES"));
        
        assertEquals("Email không hợp lệ!", ValidationUtils.validateRegister("John Doe", "invalid-email", "password123", "password123", "YES"));
        
        assertEquals("Mật khẩu phải có ít nhất 6 ký tự!", ValidationUtils.validateRegister("John Doe", "john@example.com", "123", "123", "YES"));
        
        assertEquals("Mật khẩu xác nhận không khớp!", ValidationUtils.validateRegister("John Doe", "john@example.com", "password123", "wrongpass", "YES"));
        
        assertEquals("Vui lòng chọn trạng thái mở shop!", ValidationUtils.validateRegister("John Doe", "john@example.com", "password123", "password123", ""));
        assertEquals("Vui lòng chọn trạng thái mở shop!", ValidationUtils.validateRegister("John Doe", "john@example.com", "password123", "password123", null));
    }
}
