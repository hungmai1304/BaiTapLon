package com.auction.server.service;

import java.util.HashMap;
import java.util.Map;

import com.auction.common.model.User;

public class UserService {
    private Map<String, User> userDatabase;
    public UserService() {
        userDatabase=new HashMap<>();
        // Tạo sẵn các tài khoản có đuôi @gmail.com để test
        // (Lưu ý: Tạm thời mình dùng trường username của class User để chứa email)
        userDatabase.put("admin@gmail.com", new User(1, "admin@gmail.com", "123456", "ADMIN"));
        userDatabase.put("kkkkk@gmail.com", new User(2, "kkkkk@gmail.com", "password123", "BIDDER"));
    }
    public User authenticate(String email, String password) {
        // Kiem tra dinh dang gmail bat buoc
        String emailRegex = "^[a-zA-Z0-9._%+-]+@gmail\\.com$";
        if (!email.matches(emailRegex)) {
            System.out.println("Loi: Dia chi Gmail khong hop le.");
            return null ;
        }
        User user=userDatabase.get(email);
        if (user!=null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }
}
