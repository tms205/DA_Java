package com.huit.da_java.model;

public class ProductSalesItem {
    private String name;
    private String categoryName;
    private String imageUrl;
    private int quantity;
    private double revenue;
    private double percent;

    public ProductSalesItem() {
    }

    public ProductSalesItem(String name, String categoryName, String imageUrl, int quantity, double revenue) {
        this.name = name;
        this.categoryName = categoryName;
        this.imageUrl = imageUrl;
        this.quantity = quantity;
        this.revenue = revenue;
    }

    public String getName() {
        return name;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDisplayImageUrl() {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        return "/images/products/default.jpg";
    }

    public int getQuantity() {
        return quantity;
    }

    public double getRevenue() {
        return revenue;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public String getRevenueFormatted() {
        return String.format("%,.0f VND", revenue);
    }

    public String getPercentStyle() {
        return "width: " + Math.max(4, Math.min(100, percent)) + "%";
    }
}
