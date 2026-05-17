package com.auction.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

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

    // 2. Hàm sinh ID ngẫu nhiên bảo mật (Sử dụng UUID)
    // Thay thế logic hash timestamp cũ vì nó dễ bị đoán trước (Security Issue #6)
    public static String generateRandomId() {
        return UUID.randomUUID().toString().substring(0, 12).toUpperCase();
    }

    // Giữ lại hàm này để không làm hỏng code cũ, nhưng bên trong dùng UUID
    public static String hashTimestampToId(String timestamp) {
        return generateRandomId();
    }

    // 3. Hàm tiện ích: Trả về một mảng String [ID, Time] để dùng ngay
    public static String[] generateFullInfo() {
        String time = getCurrentTimestamp();
        String id = generateRandomId();
        return new String[]{id, time};
    }
}