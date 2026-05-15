package com.auction.common.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import static org.junit.jupiter.api.Assertions.*;

class LocalDateTimeAdapterTest {

    private final LocalDateTimeAdapter adapter = new LocalDateTimeAdapter();

    @Test
    void testSerialize() {
        LocalDateTime now = LocalDateTime.of(2026, 5, 14, 10, 0, 0);
        JsonElement element = adapter.serialize(now, null, null);
        assertTrue(element instanceof JsonPrimitive);
        assertEquals("2026-05-14T10:00:00", element.getAsString());
    }

    @Test
    void testDeserialize() {
        JsonElement element = new JsonPrimitive("2026-05-14T10:00:00");
        LocalDateTime result = adapter.deserialize(element, null, null);
        assertEquals(LocalDateTime.of(2026, 5, 14, 10, 0, 0), result);
    }

    @Test
    void testRoundTrip() {
        LocalDateTime original = LocalDateTime.now();
        JsonElement serialized = adapter.serialize(original, null, null);
        LocalDateTime deserialized = adapter.deserialize(serialized, null, null);
        // ISO_LOCAL_DATE_TIME might lose some precision if not careful, but usually it's fine for LocalDateTime
        assertEquals(original, deserialized);
    }
}
