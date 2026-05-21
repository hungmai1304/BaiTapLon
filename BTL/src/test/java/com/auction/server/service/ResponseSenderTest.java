package com.auction.server.service;

import org.java_websocket.WebSocket;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

//Check dữ liệu Json gửi qua WebSocket đúng định dạng

class ResponseSenderTest {

    @Test
    void testSend_Success() {
        WebSocket mockConn = mock(WebSocket.class);
        when(mockConn.isOpen()).thenReturn(true);

        ResponseSender.send(mockConn, "TEST_TYPE", "SUCCESS", "Test message", null);

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(mockConn).send(captor.capture());
        
        String sentJson = captor.getValue();
        assertTrue(sentJson.contains("\"type\":\"TEST_TYPE\""));
        assertTrue(sentJson.contains("\"status\":\"SUCCESS\""));
        assertTrue(sentJson.contains("\"message\":\"Test message\""));
    }

    @Test
    void testSend_ClosedConnection() {
        WebSocket mockConn = mock(WebSocket.class);
        when(mockConn.isOpen()).thenReturn(false);

        ResponseSender.send(mockConn, "TEST_TYPE", "SUCCESS", "Test message", null);

        verify(mockConn, never()).send(anyString());
    }

    @Test
    void testSend_NullConnection() {
        ResponseSender.send(null, "TEST_TYPE", "SUCCESS", "Test message", null);
        // Should not throw NPE
    }
}
