package com.huit.da_java.service;

import java.time.LocalDateTime;

public record OrderNotification(
        int orderId,
        String customerName,
        String totalFormatted,
        String note,
        LocalDateTime createdAt
) {
}
