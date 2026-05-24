package com.huit.da_java.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public final class DatabaseConnection {
    private static final String DEFAULT_URL =
            "jdbc:sqlserver://TMS;instanceName=MSSQLSERVER01;databaseName=CAFEMANAGEMENT;encrypt=true;trustServerCertificate=true";
    private static final String DEFAULT_USER = "sa";
    private static final String DEFAULT_PASSWORD = "123";

    private DatabaseConnection() {
    }

    public static Connection getConnection() throws SQLException {
        String url = firstNonBlank(System.getenv("DB_URL"), System.getProperty("db.url"), DEFAULT_URL);
        String user = firstNonBlank(System.getenv("DB_USERNAME"), System.getProperty("db.username"), DEFAULT_USER);
        String password = firstNonBlank(System.getenv("DB_PASSWORD"), System.getProperty("db.password"), DEFAULT_PASSWORD);
        return DriverManager.getConnection(url, user, password);
    }

    private static String firstNonBlank(String first, String second, String fallback) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return fallback;
    }
}
