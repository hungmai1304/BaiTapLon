package com.auction.server.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class UserAuth {
	// lưu trữ dữ liệu theo Type
	public static void saveUserToDB(String email, String plainPassword) {
		String sql = "INSERT INTO users (email, password, user_type) VALUES (?, ?, ?)";

		// mã hóa mật khẩu
		String hashedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());

		int pos = email.indexOf('@');
		String type = email.substring(pos + 1);

		try {
			Connection conn = Db.getConnection();
			PreparedStatement pstmt = conn.prepareStatement(sql);
			pstmt.setString(1, email);
			pstmt.setString(2, hashedPassword);
			if (type.equals("gmail.com")) {
				pstmt.setString(3, "VNU");
			} else {
				pstmt.setString(3, "Other");
			}

			pstmt.executeUpdate();

			System.out.println("=> Đã lưu dữ liệu thành công: [" + email + "]");

		} catch (SQLException e) {
			System.out.println("Lỗi khi lưu dữ liệu: " + e.getMessage());
		}
	}

	// Chạy thử
	public static void main(String[] args) {
		System.out.println("Bắt đầu lưu thử dữ liệu xuống MySQL...");

		saveUserToDB("abc@gmail.com", "eujen3f94fn");

		saveUserToDB("svbk@vnu.edu.vn", "123456");
	}
}
