package com.auction.server.db;

import io.github.cdimascio.dotenv.Dotenv;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
	private static Connection connection;

	public static Connection getConnection() {
		try {
			// Ch? t?o k?t n?i m?i n?u ch?a c� ho?c ?� b? ?�ng
			if (connection == null || connection.isClosed()) {
				// T?i c�c bi?n t? file .env
				Dotenv dotenv =  Dotenv
						.configure()
						.ignoreIfMissing()
						.load();
				String DB_URL = dotenv.get("DB_URL");
				String DB_USER = dotenv.get("DB_USER");
				String DB_PASSWORD = dotenv.get("DB_PASSWORD");

				// ??ng k� Driver PostgreSQL
				Class.forName("org.postgresql.Driver");

				// M? k?t n?i
				connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
				System.out.println("K?t n?i Database Render th�nh c�ng!");
			}
		} catch (ClassNotFoundException e) {
			System.err.println("Kh�ng t�m th?y Driver PostgreSQL. Ki?m tra l?i pom.xml!");
		} catch (SQLException e) {
			System.err.println("L?i k?t n?i Database: " + e.getMessage());
		} catch (Exception e) {
			System.err.println("L?i kh�ng x�c ??nh ho?c kh�ng t�m th?y file .env: " + e.getMessage());
		}

		return connection;

	}
}