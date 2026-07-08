package com.huit.da_java.dao;

import com.huit.da_java.database.DatabaseConnection;
import com.huit.da_java.model.CustomerFeedback;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class FeedbackDAO {
    private static final String MIGRATION_MESSAGE =
            "Database chưa có bảng CustomerFeedback. Vui lòng chạy src/main/resources/db/customer-feedback-migration.sql.";

    public void add(int customerId, String reason, String expectation) throws SQLException {
        requireFeedbackTable();
        String sql = """
                INSERT INTO CustomerFeedback (customer_id, reason, expectation)
                VALUES (?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, customerId);
            stmt.setString(2, reason);
            stmt.setString(3, expectation);
            stmt.executeUpdate();
        }
    }

    public List<CustomerFeedback> getRecent(int limit) throws SQLException {
        requireFeedbackTable();
        String sql = """
                SELECT TOP (?) f.feedback_id,
                               f.customer_id,
                               c.full_name,
                               c.phone,
                               c.email,
                               f.reason,
                               f.expectation,
                               f.created_at
                FROM CustomerFeedback f
                INNER JOIN Customer c ON c.customer_id = f.customer_id
                ORDER BY f.created_at DESC, f.feedback_id DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, Math.max(1, limit));
            try (ResultSet rs = stmt.executeQuery()) {
                List<CustomerFeedback> feedbackList = new ArrayList<>();
                while (rs.next()) {
                    feedbackList.add(mapFeedback(rs));
                }
                return feedbackList;
            }
        }
    }

    public int countAll() throws SQLException {
        requireFeedbackTable();
        String sql = "SELECT COUNT(*) AS total FROM CustomerFeedback";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    private synchronized void requireFeedbackTable() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT OBJECT_ID(N'dbo.CustomerFeedback', N'U') AS feedback_table")) {
            if (!rs.next() || rs.getObject("feedback_table") == null) {
                throw new SQLException(MIGRATION_MESSAGE);
            }
        }
    }

    private CustomerFeedback mapFeedback(ResultSet rs) throws SQLException {
        CustomerFeedback feedback = new CustomerFeedback();
        feedback.setFeedbackId(rs.getInt("feedback_id"));
        feedback.setCustomerId(rs.getInt("customer_id"));
        feedback.setCustomerName(rs.getString("full_name"));
        feedback.setCustomerPhone(rs.getString("phone"));
        feedback.setCustomerEmail(rs.getString("email"));
        feedback.setReason(rs.getString("reason"));
        feedback.setExpectation(rs.getString("expectation"));
        feedback.setCreatedAt(String.valueOf(rs.getTimestamp("created_at")));
        return feedback;
    }
}
