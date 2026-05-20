package com.auction.client.controller;

import com.auction.common.model.user.User;

public class SomeGlobal {
    private static HomeController homeController;
    private static MainController mainController;
    private static ShopSellController shopSellController;
    private static User currentUser;
    private static BankController bankController;
    private static TopViewController topViewController;

    public static void setTopViewController(TopViewController controller) {
        topViewController = controller;
    }

    public static TopViewController getTopViewController() {
        return topViewController;
    }

    // --- BIẾN VÀ HÀM CHO ADMIN CONTROLLER TẠI ĐÂY ---
    private static AdminUserOnlineController adminUserOnlineController;
    private static AdminMainController adminMainController; // Thêm biến adminMainController

    public static void setAdminMainController(AdminMainController controller) {
        adminMainController = controller;
    }

    public static AdminMainController getAdminMainController() {
        return adminMainController;
    }

    public static void setAdminUserOnlineController(AdminUserOnlineController controller) {
        adminUserOnlineController = controller;
    }

    public static AdminUserOnlineController getAdminUserOnlineController() {
        return adminUserOnlineController;
    }
    // -----------------------------------------------------

    public static void setBankController(BankController controller) {
        bankController = controller;
    }

    public static BankController getBankController() {
        return bankController;
    }

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
}