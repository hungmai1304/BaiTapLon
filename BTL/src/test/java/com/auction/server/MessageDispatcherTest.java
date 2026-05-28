package com.auction.server;

import com.auction.protocol.MessageType;
import com.auction.protocol.Request;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class MessageDispatcherTest {

    private MessageDispatcher dispatcher;
    private Gson gson;
    private ServerContext context;
    private WebSocket conn;

    @BeforeEach
    void setUp() {
        gson = new Gson();
        context = mock(ServerContext.class);
        conn = mock(WebSocket.class);
        dispatcher = new MessageDispatcher(gson, context);
    }

    @Test
    @DisplayName("Test unauthorized access attempt")
    void testUnauthorizedAccess() {
        // Prepare a request that is NOT LOGIN or REGISTER
        Request request = new Request(MessageType.PLACE_BID_REQUEST);
        String message = gson.toJson(request);

        // Mock context to return null (not online)
        when(context.getUserByConn(conn)).thenReturn(null);

        dispatcher.dispatch(conn, message);

        // Verify that error message was sent to connection
        verify(conn).send(contains("UNAUTHORIZED_PLEASE_LOGIN"));
    }

    @Test
    @DisplayName("Test authorized bypass for LOGIN_REQUEST")
    void testLoginBypass() {
        // Prepare a LOGIN_REQUEST
        Request request = new Request(MessageType.LOGIN_REQUEST);
        String message = gson.toJson(request);

        // Mock context to return null (not online yet)
        when(context.getUserByConn(conn)).thenReturn(null);

        dispatcher.dispatch(conn, message);

        // Verify that it did NOT send UNAUTHORIZED error
        verify(conn, never()).send(contains("UNAUTHORIZED_PLEASE_LOGIN"));
    }

    @Test
    @DisplayName("Test authorized access for online user")
    void testOnlineUserAccess() {
        // Prepare any request
        Request request = new Request(MessageType.GET_BALANCE_REQUEST);
        String message = gson.toJson(request);

        // Mock context to return a userId (online)
        when(context.getUserByConn(conn)).thenReturn("user@example.com");

        dispatcher.dispatch(conn, message);

        // Verify that it did NOT send UNAUTHORIZED error
        verify(conn, never()).send(contains("UNAUTHORIZED_PLEASE_LOGIN"));
    }

    @Test
    @DisplayName("Test invalid JSON handling")
    void testInvalidJson() {
        String message = "{ invalid json }";

        dispatcher.dispatch(conn, message);

        // Verify that INVALID_JSON error was sent
        verify(conn).send(contains("INVALID_JSON"));
    }
}
