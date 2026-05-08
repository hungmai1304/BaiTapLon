package com.auction.client.controller;

public class SomeGlobal {
    private static HomeController homeController;

    public static void setHomeController(HomeController controller) {
        homeController = controller;
    }

    public static HomeController getHomeController() {
        return homeController;
    }
}
