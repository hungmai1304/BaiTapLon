package com.auction.client.controller.general;

import com.auction.client.controller.admin.AdminMainController;
import com.auction.client.controller.admin.AdminUserOnlineController;
import com.auction.client.controller.admin.BannedListController;
import com.auction.client.controller.mainHome.BankController;
import com.auction.client.controller.mainHome.HomeController;
import com.auction.client.controller.mainHome.MainController;
import com.auction.client.controller.shop.ShopSellController;
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
    private static AdminMainController adminMainController;
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

    /**
     * BỔ SUNG THÊM: Hàm dọn dẹp sạch sẽ dữ liệu Global khi Logout.
     * Tránh việc lưu vết Controller cũ gây lỗi logic khi đăng nhập bằng tài khoản khác.
     */
    public static void clearAll() {
        homeController = null;
        mainController = null;
        shopSellController = null;
        currentUser = null;
        bankController = null;
        adminUserOnlineController = null;
        adminMainController = null;
        bannedListController = null;
        System.out.println("[SomeGlobal] Đã xoá toàn bộ references của các bộ điều khiển hệ thống (Clear session thành công).");
    }
}