package com.huit.da_java.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private static final DateTimeFormatter ORDER_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private int orderId;
    private int tableId;
    private String tableName;
    private int userId;
    private String userName;
    private int customerId;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private String customerAddress;
    private String orderTime;
    private String status;
    private double total;
    private String orderType;
    private String deliveryAddress;
    private String customerNote;
    private String requestedPaymentMethod;
    private String paymentMethod;
    private double amountPaid;
    private String paymentTime;
    private List<OrderDetail> details = new ArrayList<>();

    public int getOrderId() {
        return orderId;
    }

    public void setOrderId(int orderId) {
        this.orderId = orderId;
    }

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

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public String getCustomerPhone() {
        return customerPhone;
    }

    public void setCustomerPhone(String customerPhone) {
        this.customerPhone = customerPhone;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public void setCustomerEmail(String customerEmail) {
        this.customerEmail = customerEmail;
    }

    public String getCustomerAddress() {
        return customerAddress;
    }

    public void setCustomerAddress(String customerAddress) {
        this.customerAddress = customerAddress;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }

    public String getOrderTimeText() {
        if (orderTime == null || orderTime.isBlank() || "null".equals(orderTime)) {
            return "Chưa rõ";
        }
        String normalized = orderTime.replace(' ', 'T');
        try {
            return LocalDateTime.parse(normalized).format(ORDER_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            int dotIndex = normalized.indexOf('.');
            if (dotIndex > 0) {
                try {
                    return LocalDateTime.parse(normalized.substring(0, dotIndex)).format(ORDER_TIME_FORMATTER);
                } catch (DateTimeParseException ignored) {
                    return orderTime;
                }
            }
            return orderTime;
        }
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        this.total = total;
    }

    public String getOrderType() {
        return orderType;
    }

    public void setOrderType(String orderType) {
        this.orderType = orderType;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    public String getCustomerNote() {
        return customerNote;
    }

    public void setCustomerNote(String customerNote) {
        this.customerNote = customerNote;
    }

    public String getRequestedPaymentMethod() {
        return requestedPaymentMethod;
    }

    public void setRequestedPaymentMethod(String requestedPaymentMethod) {
        this.requestedPaymentMethod = requestedPaymentMethod;
    }

    public String getRequestedPaymentMethodOrDefault() {
        if (requestedPaymentMethod == null || requestedPaymentMethod.isBlank()) {
            return "";
        }
        return requestedPaymentMethod;
    }

    public List<OrderDetail> getDetails() {
        return details;
    }

    public void setDetails(List<OrderDetail> details) {
        this.details = details;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentTime() {
        return paymentTime;
    }

    public void setPaymentTime(String paymentTime) {
        this.paymentTime = paymentTime;
    }

    public String getTotalFormatted() {
        return String.format("%,.0f VND", total);
    }

    public String getAmountPaidFormatted() {
        return String.format("%,.0f VND", amountPaid);
    }

    public String getStatusText() {
        if ("paid".equals(status)) {
            return "Đã thanh toán";
        }
        if ("pending".equals(status)) {
            return "Chờ thanh toán";
        }
        if ("cancelled".equals(status)) {
            return "Đã hủy";
        }
        return status;
    }

    public String getOrderTypeText() {
        if ("delivery".equals(orderType)) {
            return "Giao tận nhà";
        }
        if ("takeaway".equals(orderType)) {
            return "Nhận tại quầy";
        }
        if ("at_shop".equals(orderType)) {
            return "Dùng tại quán";
        }
        return orderType == null || orderType.isBlank() ? "Nhận tại quầy" : orderType;
    }

    public String getRequestedPaymentMethodText() {
        return getPaymentMethodText(getRequestedPaymentMethodOrDefault(), "Chưa chọn");
    }

    public String getPaymentMethodText() {
        return getPaymentMethodText(paymentMethod, "Chưa thanh toán");
    }

    public String getPaymentTimeText() {
        if (paymentTime == null || paymentTime.isBlank() || "null".equals(paymentTime)) {
            return "-";
        }
        String normalized = paymentTime.replace(' ', 'T');
        try {
            return LocalDateTime.parse(normalized).format(ORDER_TIME_FORMATTER);
        } catch (DateTimeParseException ex) {
            int dotIndex = normalized.indexOf('.');
            if (dotIndex > 0) {
                try {
                    return LocalDateTime.parse(normalized.substring(0, dotIndex)).format(ORDER_TIME_FORMATTER);
                } catch (DateTimeParseException ignored) {
                    return paymentTime;
                }
            }
            return paymentTime;
        }
    }

    private String getPaymentMethodText(String method, String fallback) {
        if (method == null || method.isBlank()) {
            return fallback;
        }
        if ("cash".equals(method)) {
            return "Tiền mặt tại quầy";
        }
        if ("momo".equals(method)) {
            return "MoMo QR";
        }
        if ("bank".equals(method)) {
            return "Chuyển khoản";
        }
        return method;
    }

    public boolean isQrPayment() {
        String method = getRequestedPaymentMethodOrDefault();
        return "momo".equals(method) || "bank".equals(method);
    }
}
