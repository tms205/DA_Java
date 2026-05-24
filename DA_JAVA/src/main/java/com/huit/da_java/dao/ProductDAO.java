package com.huit.da_java.dao;

import com.huit.da_java.database.DatabaseConnection;
import com.huit.da_java.model.Category;
import com.huit.da_java.model.Product;
import com.huit.da_java.model.StatisticItem;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ProductDAO {
    private static boolean imageStorageChecked;
    private static boolean imageColumnAvailable;

    public List<Product> getAll() throws SQLException {
        prepareImageStorage();
        String sql = """
                SELECT p.*, c.name AS category_name%s
                FROM Product p
                LEFT JOIN Category c ON p.category_id = c.category_id
                ORDER BY c.category_id, p.name
                """.formatted(imageColumnAvailable ? "" : ", CAST(NULL AS NVARCHAR(255)) AS image_url");
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapProducts(rs);
        }
    }

    public List<Product> getAvailable() throws SQLException {
        prepareImageStorage();
        String sql = """
                SELECT p.*, c.name AS category_name%s
                FROM Product p
                LEFT JOIN Category c ON p.category_id = c.category_id
                WHERE p.is_available = 1
                ORDER BY c.category_id, p.name
                """.formatted(imageColumnAvailable ? "" : ", CAST(NULL AS NVARCHAR(255)) AS image_url");
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapProducts(rs);
        }
    }

    public List<Product> search(String keyword) throws SQLException {
        prepareImageStorage();
        String sql = """
                SELECT p.*, c.name AS category_name%s
                FROM Product p
                LEFT JOIN Category c ON p.category_id = c.category_id
                WHERE p.name LIKE ? OR c.name LIKE ?
                ORDER BY c.category_id, p.name
                """.formatted(imageColumnAvailable ? "" : ", CAST(NULL AS NVARCHAR(255)) AS image_url");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String query = "%" + keyword + "%";
            stmt.setString(1, query);
            stmt.setString(2, query);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapProducts(rs);
            }
        }
    }

    public List<Product> searchAvailable(String keyword) throws SQLException {
        prepareImageStorage();
        String sql = """
                SELECT p.*, c.name AS category_name%s
                FROM Product p
                LEFT JOIN Category c ON p.category_id = c.category_id
                WHERE p.is_available = 1
                  AND (p.name LIKE ? OR c.name LIKE ?)
                ORDER BY c.category_id, p.name
                """.formatted(imageColumnAvailable ? "" : ", CAST(NULL AS NVARCHAR(255)) AS image_url");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String query = "%" + keyword + "%";
            stmt.setString(1, query);
            stmt.setString(2, query);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapProducts(rs);
            }
        }
    }

    public List<Product> getAvailableByCategory(int categoryId) throws SQLException {
        prepareImageStorage();
        String sql = """
                SELECT p.*, c.name AS category_name%s
                FROM Product p
                LEFT JOIN Category c ON p.category_id = c.category_id
                WHERE p.is_available = 1
                  AND p.category_id = ?
                ORDER BY p.name
                """.formatted(imageColumnAvailable ? "" : ", CAST(NULL AS NVARCHAR(255)) AS image_url");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, categoryId);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapProducts(rs);
            }
        }
    }

    public List<Product> searchAvailableByCategory(String keyword, int categoryId) throws SQLException {
        prepareImageStorage();
        String sql = """
                SELECT p.*, c.name AS category_name%s
                FROM Product p
                LEFT JOIN Category c ON p.category_id = c.category_id
                WHERE p.is_available = 1
                  AND p.category_id = ?
                  AND (p.name LIKE ? OR c.name LIKE ?)
                ORDER BY p.name
                """.formatted(imageColumnAvailable ? "" : ", CAST(NULL AS NVARCHAR(255)) AS image_url");
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String query = "%" + keyword + "%";
            stmt.setInt(1, categoryId);
            stmt.setString(2, query);
            stmt.setString(3, query);
            try (ResultSet rs = stmt.executeQuery()) {
                return mapProducts(rs);
            }
        }
    }

    public List<Category> getCategories() throws SQLException {
        String sql = "SELECT category_id, name FROM Category ORDER BY name";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Category> categories = new ArrayList<>();
            while (rs.next()) {
                Category category = new Category();
                category.setCategoryId(rs.getInt("category_id"));
                category.setName(rs.getString("name"));
                categories.add(category);
            }
            return categories;
        }
    }

    public boolean add(Product product) throws SQLException {
        prepareImageStorage();
        if (isBlank(product.getImageUrl())) {
            product.setImageUrl(resolveDefaultImageUrl(product.getName(), null));
        }
        String sql = imageColumnAvailable
                ? "INSERT INTO Product (name, price, category_id, is_available, image_url) VALUES (?, ?, ?, ?, ?)"
                : "INSERT INTO Product (name, price, category_id, is_available) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindProduct(stmt, product);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean update(Product product) throws SQLException {
        prepareImageStorage();
        if (isBlank(product.getImageUrl())) {
            product.setImageUrl(resolveDefaultImageUrl(product.getName(), null));
        }
        String sql = imageColumnAvailable
                ? "UPDATE Product SET name = ?, price = ?, category_id = ?, is_available = ?, image_url = ? WHERE product_id = ?"
                : "UPDATE Product SET name = ?, price = ?, category_id = ?, is_available = ? WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            bindProduct(stmt, product);
            stmt.setInt(imageColumnAvailable ? 6 : 5, product.getProductId());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean delete(int productId) throws SQLException {
        prepareImageStorage();
        String sql = "UPDATE Product SET is_available = 0 WHERE product_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, productId);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countAvailableProducts() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM Product WHERE is_available = 1";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public int countHiddenProducts() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM Product WHERE is_available = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public List<StatisticItem> getProductCountByCategory() throws SQLException {
        String sql = """
                SELECT c.name AS label,
                       COUNT(p.product_id) AS total_count,
                       CAST(0 AS DECIMAL(12, 2)) AS total_amount
                FROM Category c
                LEFT JOIN Product p ON c.category_id = p.category_id AND p.is_available = 1
                GROUP BY c.name
                ORDER BY total_count DESC, c.name
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<StatisticItem> items = new ArrayList<>();
            while (rs.next()) {
                items.add(new StatisticItem(
                        rs.getString("label"),
                        rs.getInt("total_count"),
                        rs.getDouble("total_amount")));
            }
            applyCountPercent(items);
            return items;
        }
    }

    private void applyCountPercent(List<StatisticItem> items) {
        int max = items.stream().mapToInt(StatisticItem::getCount).max().orElse(0);
        for (StatisticItem item : items) {
            item.setPercent(max <= 0 ? 0 : item.getCount() * 100.0 / max);
        }
    }

    private void bindProduct(PreparedStatement stmt, Product product) throws SQLException {
        stmt.setString(1, product.getName());
        stmt.setDouble(2, product.getPrice());
        stmt.setInt(3, product.getCategoryId());
        stmt.setBoolean(4, product.isAvailable());
        if (imageColumnAvailable) {
            stmt.setString(5, product.getImageUrl());
        }
    }

    private List<Product> mapProducts(ResultSet rs) throws SQLException {
        List<Product> products = new ArrayList<>();
        while (rs.next()) {
            products.add(mapProduct(rs));
        }
        return products;
    }

    private Product mapProduct(ResultSet rs) throws SQLException {
        Product product = new Product();
        product.setProductId(rs.getInt("product_id"));
        product.setName(rs.getString("name"));
        product.setPrice(rs.getDouble("price"));
        product.setCategoryId(rs.getInt("category_id"));
        product.setCategoryName(rs.getString("category_name"));
        product.setAvailable(rs.getBoolean("is_available"));
        product.setImageUrl(rs.getString("image_url"));
        if (isBlank(product.getImageUrl())) {
            product.setImageUrl(resolveDefaultImageUrl(product.getName(), product.getCategoryName()));
        }
        return product;
    }

    private static synchronized void prepareImageStorage() throws SQLException {
        if (imageStorageChecked) {
            return;
        }
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT COL_LENGTH(N'dbo.Product', N'image_url') AS image_column")) {
                imageColumnAvailable = rs.next() && rs.getObject("image_column") != null;
            }
        } catch (SQLException ex) {
            imageColumnAvailable = false;
        }
        imageStorageChecked = true;
    }

    private String resolveDefaultImageUrl(String name, String categoryName) {
        String text = ((name == null ? "" : name) + " " + (categoryName == null ? "" : categoryName)).toLowerCase();
        if (text.contains("trà sữa") || text.contains("milk tea")) {
            return "/images/products/milk-tea.jpg";
        }
        if (text.contains("latte") || text.contains("cappuccino")) {
            return "/images/products/latte.jpg";
        }
        if (text.contains("espresso")) {
            return "/images/products/espresso.jpg";
        }
        if (text.contains("trà") || text.contains("tea")) {
            return "/images/products/tea.jpg";
        }
        if (text.contains("nước") || text.contains("juice") || text.contains("cam")) {
            return "/images/products/juice.jpg";
        }
        if (text.contains("sinh tố") || text.contains("smoothie")) {
            return "/images/products/smoothie.jpg";
        }
        if (text.contains("bánh") || text.contains("cake")) {
            return "/images/products/cake.jpg";
        }
        if (text.contains("sandwich") || text.contains("burger") || text.contains("khoai")) {
            return "/images/products/sandwich.jpg";
        }
        if (text.contains("cà phê") || text.contains("coffee") || text.contains("bạc xỉu")) {
            return "/images/products/coffee.jpg";
        }
        return "/images/products/default.jpg";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
