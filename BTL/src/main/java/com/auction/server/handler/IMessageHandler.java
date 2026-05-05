package com.auction.server.handler;

import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import java.util.Map;

public interface IMessageHandler {
    void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context);
}