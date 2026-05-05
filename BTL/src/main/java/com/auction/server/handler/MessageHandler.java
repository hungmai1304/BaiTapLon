package com.auction.server.handler;

import org.java_websocket.WebSocket;
import com.auction.protocol.Request;

/**
 * Interface xử lý các loại message từ client.
 * Mỗi chức năng sẽ có 1 class implements riêng:
 * LoginHandler, BidHandler, ItemHandler...
 */
public interface MessageHandler {

    /**
     * Xử lý request từ client
     *
     * @param conn    kết nối websocket của client
     * @param request dữ liệu client gửi lên
     */
    void handle(WebSocket conn, Request request);
}