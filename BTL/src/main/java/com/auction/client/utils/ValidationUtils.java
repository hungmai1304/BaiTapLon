package com.auction.client.utils;

public class ValidationUtils {

    public static boolean isValidCredentials(String email, String password) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        boolean isEmailValid = email != null && email.matches(emailRegex);
        boolean isPasswordValid = password != null && password.length() >= 6;

        return isEmailValid && isPasswordValid;
    }

    public static String validateRegister(String name, String email, String password, String reconfirm, String shopChoice) {
        if (name == null || name.trim().isEmpty()) {
            return "Tên không được để trống!";
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@(.+)$";
        if (email == null || !email.matches(emailRegex)) {
            return "Email không hợp lệ!";
        }

        if (password == null || password.length() < 6) {
            return "Mật khẩu phải có ít nhất 6 ký tự!";
        }

        if (!password.equals(reconfirm)) {
            return "Mật khẩu xác nhận không khớp!";
        }

        if (shopChoice == null || shopChoice.isEmpty()) {
            return "Vui lòng chọn trạng thái mở shop!";
        }

        return "VALID";
    }
}