package com.auction.server.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertNull;

//test các TH xử lí ảnh qua Base64

class FileServiceTest {

    @Test
    void testSaveImage_NullBase64() {
        String url = FileService.saveImage(null, "P001");
        assertNull(url);
    }

    @Test
    void testSaveImage_EmptyBase64() {
        String url = FileService.saveImage("", "P001");
        assertNull(url);
    }

}
