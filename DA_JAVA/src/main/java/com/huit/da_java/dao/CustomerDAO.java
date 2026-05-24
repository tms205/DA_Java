package com.huit.da_java.dao;

import com.huit.da_java.database.DatabaseConnection;
import com.huit.da_java.model.Customer;
import com.huit.da_java.service.CustomerPasswordService;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CustomerDAO {
    private static final String ACCOUNT_MIGRATION_MESSAGE =
            "Database chưa có đủ cột tài khoản khách hàng/xóa mềm. Vui lòng chạy src/main/resources/db/otp-email-migration.sql.";
    private final CustomerPasswordService passwordService;

    public CustomerDAO(CustomerPasswordService passwordService) {
        this.passwordService = passwordService;
    }

    public int register(Customer customer) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = """
                INSERT INTO Customer (phone, password, full_name, email, address)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, customer.getPhone());
            stmt.setString(2, passwordService.encode(customer.getPassword()));
            stmt.setString(3, customer.getFullName());
            stmt.setString(4, customer.getEmail());
            stmt.setString(5, customer.getAddress());
            stmt.executeUpdate();
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public Customer login(String phone, String password) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = """
                SELECT customer_id, phone, password, full_name, email, address, total_spent, is_active, created_at
                FROM Customer
                WHERE phone = ? AND is_active = 1 AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            Customer customer;
            String storedPassword;
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                storedPassword = rs.getString("password");
                if (!passwordService.matches(password, storedPassword)) {
                    return null;
                }
                customer = mapCustomer(rs);
            }
            if (passwordService.needsUpgrade(storedPassword)) {
                updatePasswordHash(conn, customer.getCustomerId(), passwordService.encode(password));
            }
            return customer;
        }
    }

    public Customer getById(int customerId) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = "SELECT * FROM Customer WHERE customer_id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapCustomer(rs) : null;
            }
        }
    }

    public boolean existsByPhone(String phone) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = "SELECT COUNT(*) AS total FROM Customer WHERE phone = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    public boolean existsByEmail(String email) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = "SELECT COUNT(*) AS total FROM Customer WHERE LOWER(ISNULL(email, '')) = LOWER(?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() && rs.getInt("total") > 0;
            }
        }
    }

    public Customer findActiveByPhoneAndEmail(String phone, String email) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = """
                SELECT customer_id, phone, full_name, email, address, total_spent, is_active, created_at
                FROM Customer
                WHERE phone = ?
                  AND LOWER(ISNULL(email, '')) = LOWER(?)
                  AND is_active = 1
                  AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, phone);
            stmt.setString(2, email);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapCustomer(rs) : null;
            }
        }
    }

    public boolean update(Customer customer) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = "UPDATE Customer SET phone = ?, full_name = ?, email = ?, address = ? WHERE customer_id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, customer.getPhone());
            stmt.setString(2, customer.getFullName());
            stmt.setString(3, customer.getEmail());
            stmt.setString(4, customer.getAddress());
            stmt.setInt(5, customer.getCustomerId());
            return stmt.executeUpdate() > 0;
        }
    }

    public List<Customer> getAll() throws SQLException {
        prepareCustomerAccountColumns();
        String sql = """
                SELECT customer_id, phone, full_name, email, address, created_at, total_spent, is_active
                FROM Customer
                WHERE is_deleted = 0
                ORDER BY created_at DESC, customer_id DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Customer> customers = new ArrayList<>();
            while (rs.next()) {
                customers.add(mapCustomer(rs));
            }
            return customers;
        }
    }

    public List<Customer> search(String keyword) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = """
                SELECT customer_id, phone, full_name, email, address, created_at, total_spent, is_active
                FROM Customer
                WHERE is_deleted = 0
                  AND (full_name LIKE ? OR phone LIKE ? OR email LIKE ? OR address LIKE ?)
                ORDER BY created_at DESC, customer_id DESC
                """;
        String pattern = "%" + keyword + "%";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                stmt.setString(i, pattern);
            }
            try (ResultSet rs = stmt.executeQuery()) {
                List<Customer> customers = new ArrayList<>();
                while (rs.next()) {
                    customers.add(mapCustomer(rs));
                }
                return customers;
            }
        }
    }

    public int countActiveCustomers() throws SQLException {
        return countByActive(true);
    }

    public int countInactiveCustomers() throws SQLException {
        return countByActive(false);
    }

    public boolean resetPassword(String phone, String email, String newPassword) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = """
                UPDATE Customer
                SET password = ?
                WHERE phone = ?
                  AND LOWER(ISNULL(email, '')) = LOWER(?)
                  AND is_active = 1
                  AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordService.encode(newPassword));
            stmt.setString(2, phone);
            stmt.setString(3, email);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean toggleActive(int customerId) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = "UPDATE Customer SET is_active = CASE WHEN is_active = 1 THEN 0 ELSE 1 END WHERE customer_id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean softDelete(int customerId) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = "UPDATE Customer SET is_deleted = 1, is_active = 0 WHERE customer_id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            return stmt.executeUpdate() > 0;
        }
    }

    private int countByActive(boolean active) throws SQLException {
        prepareCustomerAccountColumns();
        String sql = "SELECT COUNT(*) AS total FROM Customer WHERE is_active = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBoolean(1, active);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    private void updatePasswordHash(Connection conn, int customerId, String passwordHash) throws SQLException {
        String sql = "UPDATE Customer SET password = ? WHERE customer_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, passwordHash);
            stmt.setInt(2, customerId);
            stmt.executeUpdate();
        }
    }

    private synchronized void prepareCustomerAccountColumns() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT COL_LENGTH(N'dbo.Customer', N'password') AS password_column,
                            COL_LENGTH(N'dbo.Customer', N'email') AS email_column,
                            COL_LENGTH(N'dbo.Customer', N'is_active') AS active_column,
                            COL_LENGTH(N'dbo.Customer', N'is_deleted') AS deleted_column
                     """)) {
            if (!rs.next()
                    || rs.getObject("password_column") == null
                    || rs.getObject("email_column") == null
                    || rs.getObject("active_column") == null
                    || rs.getObject("deleted_column") == null) {
                throw new SQLException(ACCOUNT_MIGRATION_MESSAGE);
            }
        }
    }

    private Customer mapCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setPhone(rs.getString("phone"));
        customer.setFullName(rs.getString("full_name"));
        customer.setEmail(rs.getString("email"));
        customer.setAddress(rs.getString("address"));
        try {
            customer.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        } catch (SQLException ignored) {
            customer.setCreatedAt(null);
        }
        try {
            customer.setActive(rs.getBoolean("is_active"));
        } catch (SQLException ignored) {
            customer.setActive(true);
        }
        try {
            customer.setTotalSpent(rs.getDouble("total_spent"));
        } catch (SQLException ignored) {
            customer.setTotalSpent(0);
        }
        return customer;
    }
}
