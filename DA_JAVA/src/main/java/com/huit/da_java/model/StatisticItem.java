package com.huit.da_java.model;

public class StatisticItem {
    private String label;
    private int count;
    private double amount;
    private double percent;

    public StatisticItem() {
    }

    public StatisticItem(String label, int count, double amount) {
        this.label = label;
        this.count = count;
        this.amount = amount;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public double getPercent() {
        return percent;
    }

    public void setPercent(double percent) {
        this.percent = percent;
    }

    public String getAmountFormatted() {
        return String.format("%,.0f VND", amount);
    }

    public String getPercentStyle() {
        return "width: " + Math.max(2, Math.min(100, percent)) + "%";
    }

    public String getHeightStyle() {
        return "height: " + Math.max(8, Math.min(100, percent)) + "%";
    }
}
