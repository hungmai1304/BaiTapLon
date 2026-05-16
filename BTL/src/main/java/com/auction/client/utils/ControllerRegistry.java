package com.auction.client.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ControllerRegistry {
    //attributes
    private static final Map<String, Object> registry = new ConcurrentHashMap<>();
    //contructor
    private ControllerRegistry() {}
    //-----------------------------------------------------------------------------------

    public static void register(String key, Object controller) {
        registry.put(key, controller);
        System.out.println("[ControllerRegistry] Đã đăng ký Controller: " + key);
    }
//-----------------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    public static <T> T get(String key) {
        return (T) registry.get(key);
    }
//-----------------------------------------------------------------------------------

    public static void unregister(String key) {
        registry.remove(key);
    }
}