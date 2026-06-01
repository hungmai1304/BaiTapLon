package com.auction.server.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.util.Map;
import java.util.logging.Logger;

public class FileService {
    private static final Logger LOGGER = Logger.getLogger(FileService.class.getName());
    // Thông tin lấy từ Dashboard Cloudinary của mày
    private static final Cloudinary cloudinary = new Cloudinary(ObjectUtils.asMap(
            "cloud_name", "dlylyya7s",
            "api_key", "962659778668757",
            "api_secret", "deA_L_PE49UN4iX23Rtd8lx09vw" // Đã dán hộ mày luôn
    ));

    /**
     * Đẩy ảnh Base64 lên Cloudinary và lấy URL
     */
    public static String saveImage(String base64, String productId) {
        try {
            if (base64 == null || base64.isEmpty()) return null;

            // Cloudinary cần prefix này để nhận diện Base64
            String dataUrl = "data:image/png;base64," + base64;

            // Upload lên mây
            Map uploadResult = cloudinary.uploader().upload(dataUrl, ObjectUtils.asMap(
                    "public_id", "product_" + productId, // Tên file trên cloud
                    "overwrite", true,                    // Ghi đè nếu sửa sản phẩm
                    "resource_type", "image"
            ));

            // Trả về link URL (ví dụ: https://res.cloudinary.com/...)
            String url = (String) uploadResult.get("secure_url");
            LOGGER.info("[Cloudinary] Upload thành công: " + url);
            return url;

        } catch (Exception e) {
            LOGGER.severe("[Cloudinary ERROR] Không thể upload ảnh: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // Hàm này ĐÉO CẦN NỮA vì ảnh nằm trên mây rồi, Client tự load qua URL
    /*
    public static String readImageAsBase64(String path) {
        return null;
    }
    */
}