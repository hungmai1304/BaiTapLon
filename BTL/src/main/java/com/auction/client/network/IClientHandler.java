package com.auction.client.network;

import com.auction.protocol.Response;

public interface IClientHandler {
    void handle(Response response);
}