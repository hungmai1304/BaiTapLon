package com.auction.client.utils;

public class ValidationUtils {
    public static boolean isValidCredentials(String email, String password) {
        // Check email bằng Regex (đảm bảo có dạng abc@xyz.com)
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        boolean isEmailValid = email.matches(emailRegex);

        // Check độ dài mật khẩu
        boolean isPasswordValid = password != null && password.length() >= 6;

        return isEmailValid && isPasswordValid;
    }
}
