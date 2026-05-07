package com.auction.client.annotation;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ResponseHandler {
    String type(); // Ví dụ: "LOGIN_RESPONSE", "BID_RESPONSE"
}