package com.auction.client.network;

import com.auction.protocol.Response;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientMessageDispatcherTest {

    private final PrintStream standardOut = System.out;
    private final PrintStream standardErr = System.err;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outputStreamCaptor));
        System.setErr(new PrintStream(errStreamCaptor));
    }

    @AfterEach
    public void tearDown() {
        System.setOut(standardOut);
        System.setErr(standardErr);
    }

    @Test
    void testDispatchNullMessage() {
        ClientMessageDispatcher.dispatch(null);
        // Should return early and not crash
    }

    @Test
    void testDispatchInvalidJson() {
        ClientMessageDispatcher.dispatch("invalid json");
        assertTrue(errStreamCaptor.toString().contains("Lỗi Parse:"));
    }

    @Test
    void testDispatchUnhandledType() {
        // Create a valid JSON but with a type that has no registered handler
        Response res = new Response();
        res.setType("UNKNOWN_NON_EXISTENT_TYPE_123");
        String json = ClientMessageDispatcher.gson.toJson(res);
        
        ClientMessageDispatcher.dispatch(json);
        
        assertTrue(errStreamCaptor.toString().contains("Chưa đăng ký Handler cho: UNKNOWN_NON_EXISTENT_TYPE_123"));
    }
}
