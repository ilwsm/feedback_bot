package org.example;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class JPAUtil {

    private static final Logger log = LoggerFactory.getLogger(JPAUtil.class);

    public static EntityManagerFactory createEntityManagerFactory(Properties cfg) {

        String baseUrl = cfg.getProperty("db_url");
        String dbName = cfg.getProperty("db_name");
        String dbUser = cfg.getProperty("db_user");
        String dbPassword = cfg.getProperty("db_password");

        createDatabaseIfNotExists(baseUrl, dbUser, dbPassword, dbName);

        String url = joinUrl(baseUrl, dbName);

        Map<String, String> props = new HashMap<>();
        props.put("jakarta.persistence.jdbc.url", url);
        props.put("jakarta.persistence.jdbc.user", dbUser);
        props.put("jakarta.persistence.jdbc.password", dbPassword);
        props.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");

        return Persistence.createEntityManagerFactory("feedbackPU", props);
    }

    private static void createDatabaseIfNotExists(String baseUrl, String user, String password, String dbName) {
        String serverUrl = joinUrl(baseUrl, "postgres");

        try (Connection connection = DriverManager.getConnection(serverUrl, user, password);
             Statement statement = connection.createStatement()) {

            String checkDbQuery = String.format(
                    "SELECT 1 FROM pg_database WHERE datname = '%s'", dbName
            );

            var resultSet = statement.executeQuery(checkDbQuery);

            if (!resultSet.next()) {
                statement.executeUpdate("CREATE DATABASE vgr_feedback");
                log.debug("Database '" + dbName + "' created successfully");
            } else {
                log.debug("Database '" + dbName + "' already exists");
            }

        } catch (SQLException e) {
            log.error("Error creating database: " + e.getMessage());
            throw new RuntimeException("Failed to create database", e);
        }
    }

    private static String joinUrl(String base, String part) {
        base = base.trim();
        part = part.trim();

        boolean baseEndsWithSlash = base.endsWith("/");
        boolean partStartsWithSlash = part.startsWith("/");

        if (baseEndsWithSlash && partStartsWithSlash) {
            return base + part.substring(1);
        } else if (!baseEndsWithSlash && !partStartsWithSlash) {
            return base + "/" + part;
        } else {
            return base + part;
        }
    }
}
