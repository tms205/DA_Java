package com.huit.da_java.dao;

import com.huit.da_java.database.DatabaseConnection;
import com.huit.da_java.model.CafeTable;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
public class TableDAO {

    public List<CafeTable> getAll() throws SQLException {
        String sql = """
                SELECT table_id, table_name, status
                FROM TableCafe
                ORDER BY table_id
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<CafeTable> tables = new ArrayList<>();
            while (rs.next()) {
                CafeTable table = new CafeTable();
                table.setTableId(rs.getInt("table_id"));
                table.setTableName(rs.getString("table_name"));
                table.setStatus(rs.getString("status"));
                tables.add(table);
            }
            return tables;
        }
    }
}
