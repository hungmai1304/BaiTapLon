package com.auction.server.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE) // Chỉ định nhãn này chỉ được dán lên Class
public @interface CommandMap {
    String value(); // Nơi chứa tên lệnh (VD: "LOGIN", "BID")
}