package com.auction.client.utils;

import com.auction.common.model.user.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;
import java.util.logging.Logger;

public class AdminContext {
    private static final Logger LOGGER = Logger.getLogger(AdminContext.class.getName());
    private static AdminContext instance;

    // 1. Danh sách người dùng trực tuyến
    private final ObservableList<User> onlineUsers = FXCollections.observableArrayList();

    // 2. BỔ SUNG: Danh sách các tài khoản đang trong hàng chờ duyệt làm Admin
    private final ObservableList<User> adminRequests = FXCollections.observableArrayList();

    private AdminContext() {}

    public static synchronized AdminContext getInstance() {
        if (instance == null) {
            instance = new AdminContext();
        }
        return instance;
    }

    // --- QUẢN LÝ DANH SÁCH USER ONLINE ---
    public void setOnlineUsers(List<User> users) {
        if (users == null) return;
        if (Platform.isFxApplicationThread()) {
            this.onlineUsers.setAll(users);
        } else {
            Platform.runLater(() -> this.onlineUsers.setAll(users));
        }
    }

    public ObservableList<User> getOnlineUsers() {
        return onlineUsers;
    }


    // --- BỔ SUNG: QUẢN LÝ DANH SÁCH CHỜ DUYỆT ADMIN ---

    /**
     * Thay thế toàn bộ danh sách tài khoản chờ duyệt bằng danh sách mới nhận về từ Server.
     * @param requests Danh sách User có role ADMIN_REQUEST nhận được từ gói tin phản hồi
     */
    public void setAdminRequests(List<User> requests) {
        if (requests == null) return;

        if (Platform.isFxApplicationThread()) {
            this.adminRequests.setAll(requests);
        } else {
            Platform.runLater(() -> this.adminRequests.setAll(requests));
        }
    }

    /**
     * Lấy danh sách tài khoản chờ duyệt phục vụ cho TableView (ép trực tiếp vào UI components)
     */
    public ObservableList<User> getAdminRequests() {
        return adminRequests;
    }


    // --- HÀM CLEAR KHI ĐĂNG XUẤT ---
    public void clear() {
        if (Platform.isFxApplicationThread()) {
            onlineUsers.clear();
            adminRequests.clear(); // Làm sạch cả danh sách chờ duyệt
        } else {
            Platform.runLater(() -> {
                onlineUsers.clear();
                adminRequests.clear();
            });
        }
    }
}