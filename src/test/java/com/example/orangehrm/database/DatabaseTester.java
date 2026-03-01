package com.example.orangehrm.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.example.orangehrm.utils.TestLogger;

/**
 * Database Testing Helper - For data validation and verification in tests.
 * Commonly used in test automation to verify data changes made through UI.
 * Works with any JDBC-compatible database.
 */
public class DatabaseTester {
    
    private Connection connection;
    private String connectionString;
    private String username;
    private String password;
    
    /**
     * Initialize database connection
     */
    public DatabaseTester(String connectionString, String username, String password) {
        this.connectionString = connectionString;
        this.username = username;
        this.password = password;
        connect();
    }
    
    private void connect() {
        try {
            this.connection = DriverManager.getConnection(connectionString, username, password);
            TestLogger.info("[OK] Database connection established");
        } catch (Exception e) {
            TestLogger.error("[FAIL] Failed to connect to database", e);
            throw new RuntimeException("Database connection failed", e);
        }
    }
    
    /**
     * Execute SELECT query and return results
     */
    public List<Map<String, Object>> executeQuery(String sql) {
        List<Map<String, Object>> results = new ArrayList<>();
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();
            ResultSetMetaData metadata = rs.getMetaData();
            int columnCount = metadata.getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metadata.getColumnName(i), rs.getObject(i));
                }
                results.add(row);
            }
            
            TestLogger.success("✓ Query executed: " + results.size() + " rows returned");
            return results;
        } catch (Exception e) {
            TestLogger.error("✗ Query execution failed", e);
            throw new RuntimeException("Database query failed", e);
        }
    }
    
    /**
     * Execute UPDATE/INSERT/DELETE query
     */
    public int executeUpdate(String sql) {
        try {
            PreparedStatement stmt = connection.prepareStatement(sql);
            int rowsAffected = stmt.executeUpdate();
            TestLogger.success("✓ Update executed: " + rowsAffected + " rows affected");
            return rowsAffected;
        } catch (Exception e) {
            TestLogger.error("✗ Update execution failed", e);
            throw new RuntimeException("Database update failed", e);
        }
    }
    
    /**
     * Verify record exists
     */
    public boolean recordExists(String tableName, Map<String, String> criteria) {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM " + tableName + " WHERE ");
        List<String> conditions = new ArrayList<>();
        
        criteria.forEach((key, value) -> {
            conditions.add(key + " = '" + value.replace("'", "''") + "'");
        });
        
        sql.append(String.join(" AND ", conditions));
        List<Map<String, Object>> result = executeQuery(sql.toString());
        
        if (!result.isEmpty()) {
            long count = Long.parseLong(String.valueOf(result.get(0).values().iterator().next()));
            boolean exists = count > 0;
            TestLogger.info((exists ? "✓" : "✗") + " Record " + (exists ? "exists" : "does not exist"));
            return exists;
        }
        return false;
    }
    
    /**
     * Get single value from database
     */
    public String getSingleValue(String sql) {
        List<Map<String, Object>> result = executeQuery(sql);
        if (result.isEmpty()) {
            return null;
        }
        
        Object value = result.get(0).values().iterator().next();
        return value != null ? value.toString() : null;
    }
    
    /**
     * Assert row count
     */
    public void assertRowCount(String tableName, int expectedCount) {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        String countStr = getSingleValue(sql);
        int actualCount = Integer.parseInt(countStr);
        
        if (actualCount != expectedCount) {
            throw new AssertionError("Row count mismatch. Expected: " + expectedCount + ", Got: " + actualCount);
        }
        TestLogger.success("✓ Row count verified: " + actualCount);
    }
    
    /**
     * Assert column value
     */
    public void assertColumnValue(String tableName, String columnName, String value, String whereClause) {
        String sql = "SELECT " + columnName + " FROM " + tableName + " WHERE " + whereClause;
        String actualValue = getSingleValue(sql);
        
        if (!value.equals(actualValue)) {
            throw new AssertionError("Column value mismatch. Expected: " + value + ", Got: " + actualValue);
        }
        TestLogger.success("✓ Column value verified: " + columnName + " = " + value);
    }
    
    /**
     * Close database connection
     */
    public void disconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                TestLogger.info("✓ Database connection closed");
            }
        } catch (Exception e) {
            TestLogger.error("✗ Failed to close database connection", e);
        }
    }
}
