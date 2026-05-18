package com.auction.server.db;

// import io.github.cdimascio.dotenv.Dotenv; // T?m th?i kh�a th? vi?n Dotenv
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
	private static Connection connection;

	public static Connection getConnection() {
		try {
			// Ch? t?o k?t n?i m?i n?u ch?a c� ho?c ?� b? ?�ng
			if (connection == null || connection.isClosed()) {

				// --- COMMENT ?O?N ??C BI?N M�I TR??NG/RENDER L?I ---
				/*
				Dotenv dotenv =  Dotenv
						.configure()
						.ignoreIfMissing()
						.load();
				String DB_URL = dotenv.get("DB_URL");
				String DB_USER = dotenv.get("DB_USER");
				String DB_PASSWORD = dotenv.get("DB_PASSWORD");
				*/

				// --- C?U H�NH ???NG D?N ??N POSTGRESQL LOCAL ---
				// Nh? thay ??i 'matkhaucuaban' b?ng m?t kh?u b?n ?� t?o tr�n DBeaver nh�!
				String DB_URL = "jdbc:postgresql://localhost:5432/auction_db";
				String DB_USER = "postgres";
				String DB_PASSWORD = "enix";

				// ??ng k� Driver PostgreSQL
				Class.forName("org.postgresql.Driver");

				// M? k?t n?i
				connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
				System.out.println("====== ?� K?T N?I DATABASE LOCAL TH�NH C�NG ======");
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Kh�ng t�m th?y Driver PostgreSQL. Ki?m tra l?i pom.xml!");
		} catch (SQLException e) {
			System.err.println("L?i k?t n?i Database: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("L?i kh�ng x�c ??nh: " + e.getMessage());
		}

		return connection;
	}
}