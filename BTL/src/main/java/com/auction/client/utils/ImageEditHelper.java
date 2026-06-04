package com.auction.client.utils;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Logger;

public class ImageEditHelper {
    private static final Logger LOGGER = Logger.getLogger(ImageEditHelper.class.getName());
//1. Đọc file ảnh sang mảng byte

    public static byte[] fileToBytes(File file) throws IOException {
        return Files.readAllBytes(file.toPath());
    }
// 2. Chuyển mảng byte thành đối tượng Image của JavaFX để hiển thị lên giao diện

    public static Image bytesToImage(byte[] bytes) {
        if (bytes == null || bytes.length == 0) return null;
        return new Image(new ByteArrayInputStream(bytes));
    }
// 3. Tính năng Cắt ảnh dạng vuông để ảnh sản phẩm trên sàn luôn đồng đều
    public static Image cropToSquare(Image originalImage) {
        int width = (int) originalImage.getWidth();
        int height = (int) originalImage.getHeight();
        int size = Math.min(width, height);

        int x = (width - size) / 2;
        int y = (height - size) / 2;

        PixelReader reader = originalImage.getPixelReader();
        return new WritableImage(reader, x, y, size, size);
    }
}
