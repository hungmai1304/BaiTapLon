package com.auction.server.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import io.github.cdimascio.dotenv.Dotenv;

public class Db {
	private static Connection connection;

	public static Connection getConnection() throws SQLException {
		if (Db.connection != null) {
			return Db.connection;
		}
		Dotenv dotenv = Dotenv.load();
		String DB_URL = dotenv.get("DB_URL");
		String DB_USER = dotenv.get("DB_USER");
		String DB_PASSWORD = dotenv.get("DB_PASSWORD");

		Db.connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
		return Db.connection;
	}
}
