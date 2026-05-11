package com.auction.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Generate_id_and_timecreated {

    // 1. Hàm lấy thời gian hiện tại dưới dạng String (định dạng chuẩn)
    public static String getCurrentTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return now.format(formatter);
    }
    public static LocalDateTime getCurrentTimestamp2() {
        return LocalDateTime.now(); // Trả về đối tượng thời gian hiện tại
    }

    // 2. Hàm Hash chuỗi thời gian thành một mã ID (Sử dụng SHA-256)
    public static String hashTimestampToId(String timestamp) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(timestamp.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : encodedHash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            // Trả về 12 ký tự đầu cho gọn hoặc toàn bộ chuỗi hash
            return hexString.toString().substring(0, 12).toUpperCase();
        } catch (NoSuchAlgorithmException e) {
            return "ID_ERROR_" + System.currentTimeMillis();
        }
    }

    // 3. Hàm tiện ích: Trả về một mảng String [ID, Time] để dùng ngay
    public static String[] generateFullInfo() {
        String time = getCurrentTimestamp();
        String id = hashTimestampToId(time + System.nanoTime()); // Thêm nanoTime để tránh trùng lặp tuyệt đối
        return new String[]{id, time};
    }
}