package com.auction.client.utils;

import com.auction.common.model.user.User;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

public class AdminContext {
    private static AdminContext instance;

    // Danh sách ObservableList quản lý danh sách người dùng đang online trên giao diện Admin
    private final ObservableList<User> onlineUsers = FXCollections.observableArrayList();

    // Private constructor để chặn khởi tạo từ bên ngoài (Singleton)
    private AdminContext() {}

    /**
     * Lấy instance duy nhất của AdminContext (Thread-safe)
     */
    public static synchronized AdminContext getInstance() {
        if (instance == null) {
            instance = new AdminContext();
        }
        return instance;
    }

    // --- QUẢN LÝ DANH SÁCH USER ONLINE ---

    /**
     * Thay thế toàn bộ danh sách người dùng online hiện tại bằng danh sách mới nhận từ Server.
     * Hỗ trợ tự động kiểm tra Thread để tránh lỗi xung đột giao diện JavaFX.
     * * @param users Danh sách User mới thu thập từ hệ thống
     */
    public void setOnlineUsers(List<User> users) {
        if (users == null) return;

        if (Platform.isFxApplicationThread()) {
            this.onlineUsers.setAll(users);
        } else {
            Platform.runLater(() -> this.onlineUsers.setAll(users));
        }
    }

    /**
     * Lấy danh sách ObservableList phục vụ cho việc gán/bind trực tiếp vào các component UI như TableView, ListView
     */
    public ObservableList<User> getOnlineUsers() {
        return onlineUsers;
    }

//    /**
//     * Thêm một người dùng mới vào danh sách khi họ vừa kết nối thành công (Real-time update)
//     */
//    public void addOnlineUser(User user) {
//        if (user == null) return;
//
//        if (Platform.isFxApplicationThread()) {
//            this.onlineUsers.add(user);
//        } else {
//            Platform.runLater(() -> this.onlineUsers.add(user));
//        }
//    }

    /**
     * Xóa một người dùng khỏi danh sách khi họ ngắt kết nối/đăng xuất khỏi hệ thống
     */
    public void removeOnlineUser(User user) {
        if (user == null) return;

        if (Platform.isFxApplicationThread()) {
            this.onlineUsers.remove(user);
        } else {
            Platform.runLater(() -> this.onlineUsers.remove(user));
        }
    }

    /**
     * Trả về số lượng người dùng đang hoạt động trong hệ thống
     */
    public int getOnlineUserCount() {
        return onlineUsers.size();
    }

    /**
     * Làm sạch toàn bộ dữ liệu lưu trữ khi Admin đăng xuất
     */
    public void clear() {
        if (Platform.isFxApplicationThread()) {
            onlineUsers.clear();
        } else {
            Platform.runLater(onlineUsers::clear);
        }
    }
}