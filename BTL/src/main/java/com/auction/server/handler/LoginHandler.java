package com.auction.server.handler;

import com.auction.protocol.Request;
import com.auction.server.model.ServerContext;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;

import java.util.Map;

public class LoginHandler implements IMessageHandler{

    @Override
    public void handle(WebSocket conn, Map<String, Object> data, Gson gson, ServerContext context) {

    }
}
