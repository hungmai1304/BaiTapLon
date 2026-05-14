package com.auction.client.controller;
import com.auction.common.model.user.User;

public class SomeGlobal {
    private static HomeController homeController;
    private static MainController mainController;

    public static void setHomeController(HomeController controller) {
        homeController = controller;
    }

    public static HomeController getHomeController() {
        return homeController;
    }
    private static ShopSellController shopSellController;

    public static ShopSellController getShopSellController() {
        return shopSellController;
    }

    public static void setShopSellController(ShopSellController controller) {
        shopSellController = controller;
    }

    private static User currentUser;

    public static User getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }
    public static void setMainController(MainController ctrl) {
        mainController = ctrl;
    }

    public static MainController getMainController() {
        return mainController;
    }
}
