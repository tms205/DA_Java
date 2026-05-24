package com.huit.da_java.model;

public class CafeTable {
    private int tableId;
    private String tableName;
    private String status;

    public int getTableId() {
        return tableId;
    }

    public void setTableId(int tableId) {
        this.tableId = tableId;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isOccupied() {
        return "occupied".equals(status);
    }

    public String getDisplayName() {
        if (isOccupied()) {
            return tableName + " - đang có khách";
        }
        return tableName;
    }
}
