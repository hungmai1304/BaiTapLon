package com.auction.common.utils;

import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import static org.junit.jupiter.api.Assertions.*;

class GenerateIdAndTimeCreatedTest {

    @Test
    void testGetCurrentTimestamp() {
        String timestamp = Generate_id_and_timecreated.getCurrentTimestamp();
        assertNotNull(timestamp);
        // Verify format yyyy-MM-dd HH:mm:ss
        assertDoesNotThrow(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").parse(timestamp));
    }

    @Test
    void testGetCurrentTimestamp2() {
        LocalDateTime now = Generate_id_and_timecreated.getCurrentTimestamp2();
        assertNotNull(now);
    }

    @Test
    void testHashTimestampToId() {
        String timestamp = "2026-05-14 10:00:00";
        String id1 = Generate_id_and_timecreated.hashTimestampToId(timestamp);
        String id2 = Generate_id_and_timecreated.hashTimestampToId(timestamp);
        
        assertNotNull(id1);
        assertNotNull(id2);
        assertEquals(12, id1.length());
        assertEquals(12, id2.length());
        // Now IDs are random, so they should be different even for same input
        assertNotEquals(id1, id2);
    }

    @Test
    void testGenerateFullInfo() {
        String[] info = Generate_id_and_timecreated.generateFullInfo();
        assertEquals(2, info.length);
        assertNotNull(info[0]); // id
        assertNotNull(info[1]); // time
        assertEquals(12, info[0].length());
    }
}
