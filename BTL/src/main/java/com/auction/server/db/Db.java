package com.auction.server.db;

// import io.github.cdimascio.dotenv.Dotenv; // Temporarily commented out
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Db {
	private static final Logger LOGGER = Logger.getLogger(Db.class.getName());
	private static Connection connection;

	public static Connection getConnection() {
		try {
			// Establish a new connection only if it does not exist or has been closed
			if (connection == null || connection.isClosed()) {

				String dbUrl = "jdbc:postgresql://localhost:5432/auction_db";
				String dbUser = "postgres";
				String dbPassword = "enix";

				// Explicitly load the PostgreSQL JDBC Driver
				Class.forName("org.postgresql.Driver");

				connection = DriverManager.getConnection(dbUrl, dbUser, dbPassword);
				LOGGER.info("====== DATABASE CONNECTION ESTABLISHED SUCCESSFULLY ======");
			}
		} catch (ClassNotFoundException e) {
			LOGGER.log(Level.SEVERE, "PostgreSQL JDBC Driver not found. Please verify your dependency configuration in pom.xml!", e);
		} catch (SQLException e) {
			LOGGER.log(Level.SEVERE, "Database connection error encountered", e);
		} catch (Exception e) {
			LOGGER.log(Level.SEVERE, "An unexpected error occurred during database operations", e);
		}

		return connection;
	}
}