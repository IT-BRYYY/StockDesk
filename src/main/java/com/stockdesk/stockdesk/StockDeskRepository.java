
package com.stockdesk.stockdesk;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class StockDeskRepository {

    public List<Product> findAllProducts() throws SQLException {
        List<Product> result = new ArrayList<>();

        String sql = "SELECT * FROM products ORDER BY id";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new Product(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("category"),
                        rs.getInt("quantity"),
                        rs.getDouble("price")
                ));
            }
        }

        return result;
    }

    public List<StockMovement> findAllMovements()
            throws SQLException {

        List<StockMovement> result = new ArrayList<>();

        String sql = """
                SELECT * FROM stock_movements
                ORDER BY id DESC
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                result.add(new StockMovement(
                        rs.getInt("id"),
                        rs.getInt("product_id"),
                        rs.getString("product_name"),
                        rs.getString("type"),
                        rs.getInt("quantity"),
                        LocalDateTime.parse(
                                rs.getString("created_at"))
                ));
            }
        }

        return result;
    }

    public void addProduct(String name, String category,
                           int quantity, double price)
            throws SQLException {

        String sql = """
                INSERT INTO products
                (name, category, quantity, price)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name);
            ps.setString(2, category);
            ps.setInt(3, quantity);
            ps.setDouble(4, price);
            ps.executeUpdate();
        }
    }

    public void updateProduct(Product product)
            throws SQLException {

        // Quantity is intentionally excluded.
        // Stock changes must use IN/OUT transactions.
        String sql = """
                UPDATE products
                SET name = ?, category = ?, price = ?
                WHERE id = ?
                """;

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, product.getName());
            ps.setString(2, product.getCategory());
            ps.setDouble(3, product.getPrice());
            ps.setInt(4, product.getId());

            if (ps.executeUpdate() != 1) {
                throw new SQLException("Product not found.");
            }
        }
    }

    public void deleteProduct(int productId)
            throws SQLException {

        try (Connection conn = DatabaseManager.getConnection()) {
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }

            try (PreparedStatement ps = conn.prepareStatement(
                    "DELETE FROM products WHERE id = ?")) {

                ps.setInt(1, productId);

                if (ps.executeUpdate() != 1) {
                    throw new SQLException("Product not found.");
                }
            }
        }
    }

    public void recordMovement(int productId, String type,
                               int quantity)
            throws SQLException {

        if (!type.equals("IN") && !type.equals("OUT")) {
            throw new SQLException("Invalid movement type.");
        }

        if (quantity <= 0) {
            throw new SQLException("Quantity must be positive.");
        }

        try (Connection conn = DatabaseManager.getConnection()) {

            conn.setAutoCommit(false);

            try {
                String productName;
                int available;

                try (PreparedStatement ps = conn.prepareStatement(
                        "SELECT name, quantity FROM products WHERE id = ?")) {

                    ps.setInt(1, productId);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Product not found.");
                        }

                        productName = rs.getString("name");
                        available = rs.getInt("quantity");
                    }
                }

                long updatedQuantity = type.equals("IN")
                        ? (long) available + quantity
                        : (long) available - quantity;

                if (updatedQuantity < 0) {
                    throw new SQLException("Insufficient stock.");
                }

                if (updatedQuantity > Integer.MAX_VALUE) {
                    throw new SQLException("Stock limit exceeded.");
                }

                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE products SET quantity = ? WHERE id = ?")) {

                    ps.setInt(1, (int) updatedQuantity);
                    ps.setInt(2, productId);
                    ps.executeUpdate();
                }

                String sql = """
                        INSERT INTO stock_movements
                        (product_id, product_name, type,
                         quantity, created_at)
                        VALUES (?, ?, ?, ?, ?)
                        """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, productId);
                    ps.setString(2, productName);
                    ps.setString(3, type);
                    ps.setInt(4, quantity);
                    ps.setString(5, LocalDateTime.now().toString());
                    ps.executeUpdate();
                }

                conn.commit();

            } catch (SQLException | RuntimeException e) {
                conn.rollback();
                throw e;
            }
        }
    }
}
