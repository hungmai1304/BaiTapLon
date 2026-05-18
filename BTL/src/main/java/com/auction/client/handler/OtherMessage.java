package com.auction.client.handler;


import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;

@ResponseHandler(type = MessageType.OTHER)
public class OtherMessage implements IClientHandler {
    @Override
    public void handle(Response response) {
        System.out.println("tin nhan khong biet nguon goc: other message:"+response.getMessage());
    }
}
