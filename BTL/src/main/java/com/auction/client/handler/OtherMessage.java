package com.auction.client.handler;


import com.auction.client.annotation.ResponseHandler;
import com.auction.client.network.IClientHandler;
import com.auction.protocol.MessageType;
import com.auction.protocol.Response;

import java.util.logging.Logger;

@ResponseHandler(type = MessageType.OTHER)
public class OtherMessage implements IClientHandler {
    private static final Logger LOGGER = Logger.getLogger(OtherMessage.class.getName());
    @Override
    public void handle(Response response) {
        LOGGER.info("tin nhan khong biet nguon goc: other message:"+response.getMessage());
    }
}
