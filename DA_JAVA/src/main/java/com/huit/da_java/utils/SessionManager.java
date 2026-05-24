package com.huit.da_java.utils;

public class SessionManager {
    private static SessionManager instance;

    private int userId;
    private String username;
    private String fullName;
    private String role;
    private int customerId;

    private SessionManager() {
    }

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void loginStaff(int userId, String username, String fullName, String role) {
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.customerId = 0;
    }

    public void loginCustomer(int customerId, String phone, String fullName) {
        this.customerId = customerId;
        this.username = phone;
        this.fullName = fullName;
        this.role = "customer";
        this.userId = 0;
    }

    public void logout() {
        userId = 0;
        username = null;
        fullName = null;
        role = null;
        customerId = 0;
    }

    public boolean isLoggedIn() {
        return role != null;
    }

    public String getRole() {
        return role;
    }

    public int getUserId() {
        return userId;
    }

    public int getCustomerId() {
        return customerId;
    }

    public String getUsername() {
        return username;
    }

    public String getFullName() {
        return fullName;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    public boolean isStaff() {
        return "staff".equals(role) || "admin".equals(role);
    }

    public boolean isCustomer() {
        return "customer".equals(role);
    }
}
