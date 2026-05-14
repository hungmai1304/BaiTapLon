package com.auction.client.controller;

import com.auction.common.model.user.User;

public class SomeGlobal {
    private static HomeController homeController;
    private static User currentUser;
    private static MainController mainController;
    private static ShopSellController shopSellController;

    public static void setHomeController(HomeController controller) {
        homeController = controller;
    }

    public static HomeController getHomeController() {
        return homeController;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    public static MainController getMainController() {
        return mainController;
    }

    public static void setMainController(MainController controller) {
        mainController = controller;
    }

    public static ShopSellController getShopSellController() {
        return shopSellController;
    }

    public static void setShopSellController(ShopSellController controller) {
        shopSellController = controller;
    }
}
