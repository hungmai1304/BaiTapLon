package com.auction.client.controller.general;

import com.auction.client.controller.admin.*;
import com.auction.client.controller.mainHome.BankController;
import com.auction.client.controller.mainHome.HomeController;
import com.auction.client.controller.mainHome.MainController;
import com.auction.client.controller.mainHome.TopViewController;
import com.auction.client.controller.shop.ShopSellController;
import com.auction.common.model.user.User;

import java.util.logging.Logger;

public class SomeGlobal {
    private static final Logger LOGGER = Logger.getLogger(SomeGlobal.class.getName());
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
    private static AllShopController allShopController; // BỔ SUNG: Quản lý danh sách Shop

    public static void setAllShopController(AllShopController controller) {
        allShopController = controller;
    }

    public static AllShopController getAllShopController() {
        return allShopController;
    }

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

    public static void clearAll() {
        homeController = null;
        mainController = null;
        shopSellController = null;
        currentUser = null;
        bankController = null;
        adminUserOnlineController = null;
        adminMainController = null;
        bannedListController = null;
        allShopController = null; // BỔ SUNG KHỬ BIẾN KHI LOGOUT
        LOGGER.info("[SomeGlobal] Đã xoá toàn bộ references của các bộ điều khiển hệ thống (Clear session thành công).");
    }
    // Thêm vào danh mục các biến static của SomeGlobal
    private static AdminOnlineAuctions adminOnlineAuctionsController;

    public static void setAdminOnlineAuctionsController(AdminOnlineAuctions controller) {
        adminOnlineAuctionsController = controller;
    }

    public static AdminOnlineAuctions getAdminOnlineAuctionsController() {
        return adminOnlineAuctionsController;
    }

// Đừng quên bổ sung vào hàm clearAll() khi logout:
// adminOnlineAuctionsController = null;
}