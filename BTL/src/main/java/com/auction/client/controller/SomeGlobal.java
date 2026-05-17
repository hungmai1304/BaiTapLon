package com.auction.client.controller;

import com.auction.common.model.user.User;

public class SomeGlobal {
    private static HomeController homeController;
    private static User currentUser;
    private static MainController mainController;
    private static ShopSellController shopSellController;
    private static User currentUser;

    // --- THÊM BIẾN VÀ HÀM CHO BANK CONTROLLER TẠI ĐÂY ---
    private static BankController bankController;

    public static void setBankController(BankController controller) {
        bankController = controller;
    }

    public static BankController getBankController() {
        return bankController;
    }
    // -----------------------------------------------------
    private static ShopSellController shopSellController;

    public static void setHomeController(HomeController controller) {
        homeController = controller;
    }

    public static HomeController getHomeController() {
        return homeController;
    }

    public static ShopSellController getShopSellController() {
        return shopSellController;
    }

    public static void setShopSellController(ShopSellController controller) {
        shopSellController = controller;
    }


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
