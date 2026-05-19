package com.auction.client.controller;

import com.auction.common.model.user.User;

public class SomeGlobal {
    private static HomeController homeController;
    private static MainController mainController;
    private static ShopSellController shopSellController;
    private static User currentUser;
    private static BankController bankController;

    // --- BIẾN VÀ HÀM CHO ADMIN CONTROLLER TẠI ĐÂY ---
    private static AdminUserOnlineController adminUserOnlineController;
    private static AdminMainController adminMainController;

    // THÊM BIẾN NÀY: Quản lý BannedListController toàn cục
    private static BannedListController bannedListController;

    // Hàm set và get cho BannedListController (Đúng chuẩn form mẫu)
    public static void setBannedListController(BannedListController controller) {
        bannedListController = controller;
    }

    public static BannedListController getBannedListController() {
        return bannedListController;
    }

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