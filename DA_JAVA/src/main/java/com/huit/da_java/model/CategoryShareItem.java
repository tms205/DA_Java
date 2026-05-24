package com.huit.da_java.model;

import java.util.Locale;

public class CategoryShareItem {
    private final String label;
    private final int quantity;
    private final double revenue;
    private final double percent;
    private final double offset;
    private final String color;

    public CategoryShareItem(String label,
                             int quantity,
                             double revenue,
                             double percent,
                             double offset,
                             String color) {
        this.label = label;
        this.quantity = quantity;
        this.revenue = revenue;
        this.percent = percent;
        this.offset = offset;
        this.color = color;
    }

    public String getLabel() {
        return label;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getRevenueFormatted() {
        return String.format("%,.0f VND", revenue);
    }

    public String getPercentFormatted() {
        return String.format(Locale.US, "%.1f%%", percent);
    }

    public String getColor() {
        return color;
    }

    public String getChartStyle() {
        return String.format(Locale.US,
                "stroke: %s; stroke-dasharray: %.4f %.4f; stroke-dashoffset: -%.4f",
                color, percent, Math.max(0, 100 - percent), offset);
    }
}
