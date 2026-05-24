package com.huit.da_java.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CategoryShareItemTest {

    @Test
    void createsPercentageAndDonutSegmentStyle() {
        CategoryShareItem item = new CategoryShareItem("Coffee", 3, 100000, 25.5, 10, "#0f766e");

        assertEquals("25.5%", item.getPercentFormatted());
        assertTrue(item.getChartStyle().contains("stroke: #0f766e"));
        assertTrue(item.getChartStyle().contains("stroke-dasharray: 25.5000 74.5000"));
        assertTrue(item.getChartStyle().contains("stroke-dashoffset: -10.0000"));
    }
}
