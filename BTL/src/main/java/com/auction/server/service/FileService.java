package com.auction.server.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;

public class FileService {
    // Đường dẫn gốc lưu trữ ảnh (sẽ nằm cùng cấp với thư mục src/bin của bạn)
    private static final String IMAGE_STORE_PATH = "server_data/product_images/";

    /**
     * Lưu ảnh từ chuỗi Base64 vào ổ cứng
     * @param base64Data Chuỗi base64 nhận từ Client
     * @param productId ID sản phẩm để đặt tên file cho duy nhất
     * @return Đường dẫn file đã lưu (để lưu vào Database)
     */
    public static String saveImage(String base64Data, String productId) {
        if (base64Data == null || base64Data.isEmpty()) return null;

        try {
            // 1. Tạo thư mục nếu chưa tồn tại
            File directory = new File(IMAGE_STORE_PATH);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // 2. Giải mã Base64 thành mảng byte (101001...)
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);

            // 3. Tạo đường dẫn file (ví dụ: server_data/product_images/PROD_001.png)
            String filePath = IMAGE_STORE_PATH + productId + ".png";

            // 4. Ghi dữ liệu vào file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                fos.write(imageBytes);
            }

            System.out.println("[FileService] Đã lưu ảnh cho SP: " + productId);
            return filePath; // Trả về đường dẫn để bạn lưu vào cột image_path trong DB
        } catch (Exception e) {
            System.err.println("[FileService] Lỗi khi lưu ảnh: " + e.getMessage());
            return null;
        }
    }

    /**
     * Đọc file từ đường dẫn và chuyển thành Base64 để gửi cho Client
     * @param filePath Đường dẫn lưu trong Database
     * @return Chuỗi Base64 của ảnh
     */
    public static String readImageAsBase64(String filePath) {
        if (filePath == null || filePath.isEmpty()) return null;

        try {
            File file = new File(filePath);
            if (!file.exists()) return null;

            // Đọc toàn bộ byte của file
            byte[] fileContent = Files.readAllBytes(file.toPath());

            // Chuyển sang Base64
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            System.err.println("[FileService] Lỗi khi đọc ảnh: " + e.getMessage());
            return null;
        }
    }
}