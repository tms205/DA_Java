package com.huit.da_java.dao;

import com.huit.da_java.database.DatabaseConnection;
import com.huit.da_java.model.StatisticItem;
import com.huit.da_java.model.User;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class UserDAO {
    private static final String EMAIL_MIGRATION_MESSAGE =
            "Database chưa có đủ cột email/xóa mềm cho tài khoản nội bộ. Vui lòng chạy src/main/resources/db/otp-email-migration.sql.";

    public User login(String username, String password) throws SQLException {
        String sql = "{CALL sp_LoginStaff(?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             java.sql.CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, password);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public List<User> getAllStaff() throws SQLException {
        String sql = """
                SELECT user_id, username, full_name, %s, role, is_active
                FROM [User]
                WHERE role IN ('staff', 'admin') AND is_deleted = 0
                ORDER BY user_id
                """.formatted(emailSelectExpression());
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<User> list = new ArrayList<>();
            while (rs.next()) {
                list.add(mapUser(rs));
            }
            return list;
        }
    }

    public List<User> searchStaffByName(String keyword) throws SQLException {
        String sql = """
                SELECT user_id, username, full_name, %s, role, is_active
                FROM [User]
                WHERE role IN ('staff', 'admin')
                  AND is_deleted = 0
                  AND (full_name LIKE ? OR username LIKE ?)
                ORDER BY user_id
                """.formatted(emailSelectExpression());
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String query = "%" + keyword + "%";
            stmt.setString(1, query);
            stmt.setString(2, query);
            try (ResultSet rs = stmt.executeQuery()) {
                List<User> list = new ArrayList<>();
                while (rs.next()) {
                    list.add(mapUser(rs));
                }
                return list;
            }
        }
    }

    public boolean registerStaff(User user) throws SQLException {
        requireAccountColumns();
        String sql = """
                INSERT INTO [User] (username, password, full_name, email, role, is_active)
                VALUES (?, ?, ?, ?, ?, 1)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.setString(3, user.getFullName());
            stmt.setString(4, user.getEmail());
            stmt.setString(5, user.getRole());
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean toggleStaffStatus(int userId, boolean active) throws SQLException {
        String sql = "UPDATE [User] SET is_active = ? WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean toggleActive(int userId) throws SQLException {
        requireAccountColumns();
        String sql = "UPDATE [User] SET is_active = CASE WHEN is_active = 1 THEN 0 ELSE 1 END WHERE user_id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateRecoveryEmail(int userId, String email) throws SQLException {
        requireAccountColumns();
        String sql = "UPDATE [User] SET email = ? WHERE user_id = ? AND role IN ('staff', 'admin') AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    public User findActiveByUsernameAndEmail(String username, String email) throws SQLException {
        requireAccountColumns();
        String sql = """
                SELECT user_id, username, full_name, email, role, is_active
                FROM [User]
                WHERE username = ?
                  AND LOWER(ISNULL(email, '')) = LOWER(?)
                  AND is_active = 1
                  AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            stmt.setString(2, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapUser(rs) : null;
            }
        }
    }

    public boolean resetPassword(String username, String email, String newPassword) throws SQLException {
        requireAccountColumns();
        String sql = """
                UPDATE [User]
                SET password = ?
                WHERE username = ?
                  AND LOWER(ISNULL(email, '')) = LOWER(?)
                  AND is_active = 1
                  AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newPassword);
            stmt.setString(2, username);
            stmt.setString(3, email);
            return stmt.executeUpdate() > 0;
        }
    }

    public int countActiveStaff() throws SQLException {
        requireAccountColumns();
        String sql = "SELECT COUNT(*) AS total FROM [User] WHERE role IN ('staff', 'admin') AND is_active = 1 AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public int countInactiveStaff() throws SQLException {
        requireAccountColumns();
        String sql = "SELECT COUNT(*) AS total FROM [User] WHERE role IN ('staff', 'admin') AND is_active = 0 AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public List<StatisticItem> getStaffRevenueStats() throws SQLException {
        requireAccountColumns();
        String sql = """
                SELECT u.full_name AS label,
                       COUNT(o.order_id) AS total_count,
                       ISNULL(SUM(o.total), 0) AS total_amount
                FROM [User] u
                LEFT JOIN Orders o ON u.user_id = o.user_id AND o.status = 'paid'
                WHERE u.role IN ('staff', 'admin') AND u.is_deleted = 0
                GROUP BY u.user_id, u.full_name
                ORDER BY total_amount DESC, total_count DESC
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
            applyAmountPercent(items);
            return items;
        }
    }

    public boolean softDelete(int userId) throws SQLException {
        requireAccountColumns();
        String sql = """
                UPDATE [User]
                SET is_deleted = 1, is_active = 0
                WHERE user_id = ? AND role = 'staff' AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
        }
    }

    private void applyAmountPercent(List<StatisticItem> items) {
        double max = items.stream().mapToDouble(StatisticItem::getAmount).max().orElse(0);
        for (StatisticItem item : items) {
            item.setPercent(max <= 0 ? 0 : item.getAmount() * 100 / max);
        }
    }

    private String emailSelectExpression() throws SQLException {
        requireAccountColumns();
        return "email";
    }

    private void requireAccountColumns() throws SQLException {
        if (!hasColumn("email") || !hasColumn("is_deleted")) {
            throw new SQLException(EMAIL_MIGRATION_MESSAGE);
        }
    }

    private boolean hasColumn(String column) throws SQLException {
        String sql = "SELECT COL_LENGTH(N'dbo.User', ?) AS value";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, column);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getObject("value") != null;
            }
        }
    }

    private User mapUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        try {
            user.setEmail(rs.getString("email"));
        } catch (SQLException ignored) {
            user.setEmail(null);
        }
        user.setRole(rs.getString("role"));
        user.setActive(rs.getBoolean("is_active"));
        return user;
    }
}
