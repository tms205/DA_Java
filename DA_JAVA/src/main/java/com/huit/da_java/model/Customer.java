/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.huit.da_java.model;


public class Customer {
    private int customerId;
    private String phone;
    private String password;
    private String fullName;
    private String email;
    private String address;
    private String createdAt;
    private double totalSpent;
    private boolean active = true;
    
    // Constructors
    public Customer() {}
    
    public Customer(String phone, String fullName, String email, String address) {
        this.phone = phone;
        this.fullName = fullName;
        this.email = email;
        this.address = address;
        this.totalSpent = 0;
    }
    
    // Getters and Setters
    public int getCustomerId() { return customerId; }
    public void setCustomerId(int customerId) { this.customerId = customerId; }
    
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    
    public double getTotalSpent() { return totalSpent; }
    public void setTotalSpent(double totalSpent) { this.totalSpent = totalSpent; }

    public String getTotalSpentFormatted() {
        return String.format("%,.0f VND", totalSpent);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    
    @Override
    public String toString() {
        return fullName + " - " + phone;
    }
}
