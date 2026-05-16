package com.auction.server.db;

// import io.github.cdimascio.dotenv.Dotenv; // Tạm thời khóa thư viện Dotenv
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
	private static Connection connection;

	public static Connection getConnection() {
		try {
			// Chỉ tạo kết nối mới nếu chưa có hoặc đã bị đóng
			if (connection == null || connection.isClosed()) {

				// --- COMMENT ĐOẠN ĐỌC BIẾN MÔI TRƯỜNG/RENDER LẠI ---
				/*
				Dotenv dotenv =  Dotenv
						.configure()
						.ignoreIfMissing()
						.load();
				String DB_URL = dotenv.get("DB_URL");
				String DB_USER = dotenv.get("DB_USER");
				String DB_PASSWORD = dotenv.get("DB_PASSWORD");
				*/

				// --- CẤU HÌNH ĐƯỜNG DẪN ĐẾN POSTGRESQL LOCAL ---
				// Nhớ thay đổi 'matkhaucuaban' bằng mật khẩu bạn đã tạo trên DBeaver nhé!
				String DB_URL = "jdbc:postgresql://localhost:5432/auction_db";
				String DB_USER = "postgres";
				String DB_PASSWORD = "enix";

				// Đăng ký Driver PostgreSQL
				Class.forName("org.postgresql.Driver");

				// Mở kết nối
				connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
				System.out.println("====== ĐÃ KẾT NỐI DATABASE LOCAL THÀNH CÔNG ======");
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Không tìm thấy Driver PostgreSQL. Kiểm tra lại pom.xml!");
		} catch (SQLException e) {
			System.err.println("Lỗi kết nối Database: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("Lỗi không xác định: " + e.getMessage());
		}

		return connection;
	}
}