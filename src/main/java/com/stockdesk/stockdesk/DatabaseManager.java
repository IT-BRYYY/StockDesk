
package com.stockdesk.stockdesk;

import java.sql.*;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:stockdesk.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    public static void initialize() throws SQLException {

        String productsSql = """
                CREATE TABLE IF NOT EXISTS products (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    category TEXT NOT NULL,
                    quantity INTEGER NOT NULL DEFAULT 0
                        CHECK(quantity >= 0),
                    price REAL NOT NULL DEFAULT 0
                        CHECK(price >= 0)
                )
                """;

        String movementsSql = """
                CREATE TABLE IF NOT EXISTS stock_movements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    product_id INTEGER,
                    product_name TEXT NOT NULL,
                    type TEXT NOT NULL
                        CHECK(type IN ('IN', 'OUT')),
                    quantity INTEGER NOT NULL
                        CHECK(quantity > 0),
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (product_id)
                        REFERENCES products(id)
                        ON DELETE SET NULL
                )
                """;

        try (Connection conn = getConnection();
             Statement statement = conn.createStatement()) {

            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute(productsSql);
            statement.execute(movementsSql);
        }
    }
}
