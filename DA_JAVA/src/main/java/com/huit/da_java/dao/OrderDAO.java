package com.huit.da_java.dao;

import com.huit.da_java.database.DatabaseConnection;
import com.huit.da_java.model.CategoryShareItem;
import com.huit.da_java.model.Order;
import com.huit.da_java.model.OrderDetail;
import com.huit.da_java.model.ProductSalesItem;
import com.huit.da_java.model.StatisticItem;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Repository
public class OrderDAO {
    private static final String ORDER_MIGRATION_MESSAGE =
            "Database chưa có đủ cột đơn hàng/xóa mềm. Vui lòng chạy src/main/resources/db/otp-email-migration.sql.";
    private static final String[] CATEGORY_SHARE_COLORS = {
            "#0f766e", "#14b8a6", "#f59e0b", "#f97316",
            "#2563eb", "#8b5cf6", "#ec4899", "#64748b"
    };

    public int createOrder(int tableId, int userId) throws SQLException {
        String sql = "{CALL sp_CreateOrder(?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setInt(1, tableId);
            stmt.setInt(2, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public int createCustomerOrder(int customerId,
                                   List<OrderDetail> items,
                                   String note,
                                   String orderType,
                                   String deliveryAddress,
                                   String requestedPaymentMethod) throws SQLException {
        if (items == null || items.isEmpty()) {
            throw new SQLException("Đơn hàng chưa có món.");
        }
        ensureCustomerOrderColumns();
        String orderSql = """
                INSERT INTO Orders(customer_id, order_type, delivery_address, customer_note, requested_payment_method)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int orderId;
                try (PreparedStatement stmt = conn.prepareStatement(orderSql, Statement.RETURN_GENERATED_KEYS)) {
                    stmt.setInt(1, customerId);
                    stmt.setString(2, orderType);
                    stmt.setString(3, deliveryAddress);
                    stmt.setString(4, note);
                    stmt.setString(5, requestedPaymentMethod);
                    stmt.executeUpdate();
                    try (ResultSet keys = stmt.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("Không tạo được đơn hàng.");
                        }
                        orderId = keys.getInt(1);
                    }
                }

                for (OrderDetail item : items) {
                    insertOrderDetail(conn, orderId, item.getProductId(), item.getQuantity());
                }
                conn.commit();
                return orderId;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public int createAndPayStaffOrder(int tableId, int userId, List<OrderDetail> items, String method) throws SQLException {
        int orderId = createOrder(tableId, userId);
        if (orderId <= 0) {
            throw new SQLException("Không tạo được đơn hàng.");
        }
        for (OrderDetail item : items) {
            addProductToOrder(orderId, item.getProductId(), item.getQuantity());
        }
        payOrder(orderId, method);
        return orderId;
    }

    public void addProductToOrder(int orderId, int productId, int quantity) throws SQLException {
        String sql = "{CALL sp_AddProductToOrder(?, ?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, productId);
            stmt.setInt(3, quantity);
            stmt.execute();
        }
    }

    public void payOrder(int orderId, String method) throws SQLException {
        String sql = "{CALL sp_PayOrder(?, ?)}";
        try (Connection conn = DatabaseConnection.getConnection();
             CallableStatement stmt = conn.prepareCall(sql)) {
            stmt.setInt(1, orderId);
            stmt.setString(2, method);
            stmt.execute();
        }
    }

    public void cancelOrder(int orderId) throws SQLException {
        ensureOrderCancellationSupport();
        String selectSql = """
                SELECT status, table_id
                FROM Orders
                WHERE order_id = ? AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer tableId = null;
                String status;
                try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Không tìm thấy đơn hàng.");
                        }
                        status = rs.getString("status");
                        int rawTableId = rs.getInt("table_id");
                        if (!rs.wasNull()) {
                            tableId = rawTableId;
                        }
                    }
                }
                if (!"pending".equals(status)) {
                    throw new SQLException("Chỉ có thể hủy đơn đang chờ.");
                }

                try (PreparedStatement stmt = conn.prepareStatement("UPDATE Orders SET status = 'cancelled' WHERE order_id = ? AND status = 'pending'")) {
                    stmt.setInt(1, orderId);
                    if (stmt.executeUpdate() == 0) {
                        throw new SQLException("Không thể hủy đơn hàng.");
                    }
                }
                if (tableId != null) {
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE TableCafe SET status = 'empty' WHERE table_id = ?")) {
                        stmt.setInt(1, tableId);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public void updateCustomerCheckoutOptions(int orderId,
                                              int customerId,
                                              String orderType,
                                              String deliveryAddress,
                                              String method) throws SQLException {
        if (!List.of("cash", "momo", "bank").contains(method)) {
            throw new SQLException("PhÆ°Æ¡ng thá»©c thanh toÃ¡n khÃ´ng há»£p lá»‡.");
        }
        ensureCustomerOrderColumns();
        String sql = """
                UPDATE Orders
                SET order_type = ?,
                    delivery_address = ?,
                    requested_payment_method = ?
                WHERE order_id = ?
                  AND customer_id = ?
                  AND status = 'pending'
                  AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, orderType);
            stmt.setString(2, deliveryAddress);
            stmt.setString(3, method);
            stmt.setInt(4, orderId);
            stmt.setInt(5, customerId);
            if (stmt.executeUpdate() == 0) {
                throw new SQLException("KhÃ´ng tÃ¬m tháº¥y Ä‘Æ¡n khÃ¡ch Ä‘ang chá» thanh toÃ¡n.");
            }
        }
    }

    public void payPendingCustomerOrder(int orderId, int userId, String method) throws SQLException {
        if (!List.of("cash", "momo", "bank").contains(method)) {
            throw new SQLException("Phương thức thanh toán không hợp lệ.");
        }
        String selectSql = """
                SELECT total, customer_id
                FROM Orders
                WHERE order_id = ?
                  AND customer_id IS NOT NULL
                  AND status = 'pending'
                  AND is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                double total;
                int customerId;
                try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Không tìm thấy đơn khách đang chờ thanh toán.");
                        }
                        total = rs.getDouble("total");
                        customerId = rs.getInt("customer_id");
                    }
                }
                if (total <= 0) {
                    throw new SQLException("Đơn hàng chưa có món.");
                }

                try (PreparedStatement stmt = conn.prepareStatement("UPDATE Orders SET user_id = ?, status = 'paid' WHERE order_id = ?")) {
                    stmt.setInt(1, userId);
                    stmt.setInt(2, orderId);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO Payment(order_id, method, amount_paid) VALUES (?, ?, ?)")) {
                    stmt.setInt(1, orderId);
                    stmt.setString(2, method);
                    stmt.setDouble(3, total);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = conn.prepareStatement("""
                        UPDATE Customer
                        SET total_spent = ISNULL(total_spent, 0) + ?
                        WHERE customer_id = ?
                        """)) {
                    stmt.setDouble(1, total);
                    stmt.setInt(2, customerId);
                    stmt.executeUpdate();
                }
                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    public Order getOrderById(int orderId) throws SQLException {
        ensureCustomerOrderColumns();
        String sql = """
                SELECT o.*, t.table_name, u.full_name AS user_name,
                       c.full_name AS customer_name, c.phone AS customer_phone,
                       c.email AS customer_email, c.address AS customer_address,
                       p.method AS payment_method, p.amount_paid, p.payment_time
                FROM Orders o
                LEFT JOIN TableCafe t ON o.table_id = t.table_id
                LEFT JOIN [User] u ON o.user_id = u.user_id
                LEFT JOIN Customer c ON o.customer_id = c.customer_id
                LEFT JOIN Payment p ON o.order_id = p.order_id
                WHERE o.order_id = ? AND o.is_deleted = 0
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Order order = mapOrder(rs);
                order.setDetails(getOrderDetails(orderId));
                return order;
            }
        }
    }

    public List<OrderDetail> getOrderDetails(int orderId) throws SQLException {
        String sql = """
                SELECT od.*, p.name AS product_name
                FROM OrderDetail od
                JOIN Product p ON od.product_id = p.product_id
                WHERE od.order_id = ?
                ORDER BY od.order_detail_id
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            try (ResultSet rs = stmt.executeQuery()) {
                List<OrderDetail> details = new ArrayList<>();
                while (rs.next()) {
                    OrderDetail detail = new OrderDetail();
                    detail.setOrderDetailId(rs.getInt("order_detail_id"));
                    detail.setOrderId(rs.getInt("order_id"));
                    detail.setProductId(rs.getInt("product_id"));
                    detail.setProductName(rs.getString("product_name"));
                    detail.setQuantity(rs.getInt("quantity"));
                    detail.setPrice(rs.getDouble("price"));
                    details.add(detail);
                }
                return details;
            }
        }
    }

    public double getRevenueByDate(LocalDate date) throws SQLException {
        String sql = "SELECT dbo.fn_GetRevenueByDate(?) AS revenue";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getDouble("revenue") : 0;
            }
        }
    }

    public int getPaidOrderCountByDate(LocalDate date) throws SQLException {
        String sql = """
                SELECT COUNT(*) AS total
                FROM Orders
                WHERE CAST(order_time AS DATE) = ? AND status = 'paid'
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(date));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? rs.getInt("total") : 0;
            }
        }
    }

    public int getPaidOrderCount() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM Orders WHERE status = 'paid'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public int getPendingCustomerOrderCount() throws SQLException {
        ensureCustomerOrderColumns();
        String sql = """
                SELECT COUNT(*) AS total
                FROM Orders
                WHERE status = 'pending'
                  AND customer_id IS NOT NULL
                  AND is_deleted = 0
                  AND requested_payment_method IS NOT NULL
                  AND LTRIM(RTRIM(requested_payment_method)) <> ''
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public double getTotalRevenue() throws SQLException {
        String sql = "SELECT ISNULL(SUM(total), 0) AS revenue FROM Orders WHERE status = 'paid'";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getDouble("revenue") : 0;
        }
    }

    public List<StatisticItem> getRevenueLastDays(int days) throws SQLException {
        int range = Math.max(1, days);
        LocalDate startDate = LocalDate.now().minusDays(range - 1L);
        DateTimeFormatter labelFormatter = DateTimeFormatter.ofPattern("dd/MM");
        Map<LocalDate, StatisticItem> itemsByDate = new LinkedHashMap<>();
        for (int i = 0; i < range; i++) {
            LocalDate date = startDate.plusDays(i);
            itemsByDate.put(date, new StatisticItem(labelFormatter.format(date), 0, 0));
        }

        String sql = """
                SELECT
                    CAST(o.order_time AS DATE) AS order_date,
                    COUNT(o.order_id) AS total_count,
                    ISNULL(SUM(o.total), 0) AS total_amount
                FROM Orders o
                WHERE o.status = 'paid'
                  AND CAST(o.order_time AS DATE) >= ?
                GROUP BY CAST(o.order_time AS DATE)
                ORDER BY CAST(o.order_time AS DATE)
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, Date.valueOf(startDate));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate date = rs.getDate("order_date").toLocalDate();
                    StatisticItem item = itemsByDate.get(date);
                    if (item != null) {
                        item.setCount(rs.getInt("total_count"));
                        item.setAmount(rs.getDouble("total_amount"));
                    }
                }
                List<StatisticItem> items = new ArrayList<>(itemsByDate.values());
                applyAmountPercent(items);
                return items;
            }
        }
    }

    public List<StatisticItem> getPaymentMethodStats() throws SQLException {
        String sql = """
                SELECT p.method AS label, COUNT(*) AS total_count, ISNULL(SUM(p.amount_paid), 0) AS total_amount
                FROM Payment p
                GROUP BY p.method
                ORDER BY total_amount DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<StatisticItem> items = new ArrayList<>();
            while (rs.next()) {
                items.add(new StatisticItem(
                        rs.getString("label"),
                        rs.getInt("total_count"),
                        rs.getDouble("total_amount")));
            }
            applyAmountPercent(items);
            return items;
        }
    }

    public List<CategoryShareItem> getCategoryRevenueShares() throws SQLException {
        String sql = """
                SELECT ISNULL(c.name, N'Chưa phân loại') AS label,
                       SUM(od.quantity) AS total_count,
                       SUM(od.quantity * od.price) AS total_amount
                FROM OrderDetail od
                JOIN Orders o ON od.order_id = o.order_id
                JOIN Product p ON od.product_id = p.product_id
                LEFT JOIN Category c ON p.category_id = c.category_id
                WHERE o.status = 'paid'
                GROUP BY c.name
                HAVING SUM(od.quantity * od.price) > 0
                ORDER BY total_amount DESC
                """;
        List<StatisticItem> values = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                values.add(new StatisticItem(
                        rs.getString("label"),
                        rs.getInt("total_count"),
                        rs.getDouble("total_amount")));
            }
        }
        double totalRevenue = values.stream().mapToDouble(StatisticItem::getAmount).sum();
        List<CategoryShareItem> shares = new ArrayList<>();
        double offset = 0;
        for (int index = 0; index < values.size(); index++) {
            StatisticItem value = values.get(index);
            double percent = totalRevenue <= 0 ? 0 : value.getAmount() * 100 / totalRevenue;
            shares.add(new CategoryShareItem(
                    value.getLabel(),
                    value.getCount(),
                    value.getAmount(),
                    percent,
                    offset,
                    CATEGORY_SHARE_COLORS[index % CATEGORY_SHARE_COLORS.length]));
            offset += percent;
        }
        return shares;
    }

    public List<StatisticItem> getTopSellingProducts(int limit) throws SQLException {
        List<ProductSalesItem> productSales = getTopProductSales(limit);
        List<StatisticItem> items = new ArrayList<>();
        for (ProductSalesItem product : productSales) {
            StatisticItem item = new StatisticItem(product.getName(), product.getQuantity(), product.getRevenue());
            item.setPercent(product.getPercent());
            items.add(item);
        }
        return items;
    }

    public List<ProductSalesItem> getTopProductSales(int limit) throws SQLException {
        ensureProductImageColumn();
        String sql = """
                SELECT TOP (?) p.name AS label,
                       c.name AS category_name,
                       p.image_url,
                       SUM(od.quantity) AS total_count,
                       SUM(od.quantity * od.price) AS total_amount
                FROM OrderDetail od
                JOIN Product p ON od.product_id = p.product_id
                LEFT JOIN Category c ON p.category_id = c.category_id
                JOIN Orders o ON od.order_id = o.order_id
                WHERE o.status = 'paid'
                GROUP BY p.name, c.name, p.image_url
                ORDER BY total_count DESC, total_amount DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<ProductSalesItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(new ProductSalesItem(
                            rs.getString("label"),
                            rs.getString("category_name"),
                            rs.getString("image_url"),
                            rs.getInt("total_count"),
                            rs.getDouble("total_amount")));
                }
                applyProductSalesPercent(items);
                return items;
            }
        }
    }

    public List<Order> getRecentOrders(int limit) throws SQLException {
        ensureCustomerOrderColumns();
        String sql = """
                SELECT TOP (?) o.*, t.table_name, u.full_name AS user_name,
                       c.full_name AS customer_name, c.phone AS customer_phone,
                       c.email AS customer_email, c.address AS customer_address,
                       p.method AS payment_method, p.amount_paid, p.payment_time
                FROM Orders o
                LEFT JOIN TableCafe t ON o.table_id = t.table_id
                LEFT JOIN [User] u ON o.user_id = u.user_id
                LEFT JOIN Customer c ON o.customer_id = c.customer_id
                LEFT JOIN Payment p ON o.order_id = p.order_id
                WHERE o.is_deleted = 0
                ORDER BY o.order_time DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
                return orders;
            }
        }
    }

    public List<Order> getRecentActiveOrders(int limit) throws SQLException {
        ensureCustomerOrderColumns();
        String sql = """
                SELECT TOP (?) o.*, t.table_name, u.full_name AS user_name,
                       c.full_name AS customer_name, c.phone AS customer_phone,
                       c.email AS customer_email, c.address AS customer_address,
                       p.method AS payment_method, p.amount_paid, p.payment_time
                FROM Orders o
                LEFT JOIN TableCafe t ON o.table_id = t.table_id
                LEFT JOIN [User] u ON o.user_id = u.user_id
                LEFT JOIN Customer c ON o.customer_id = c.customer_id
                LEFT JOIN Payment p ON o.order_id = p.order_id
                WHERE o.status <> 'cancelled'
                  AND o.is_deleted = 0
                ORDER BY o.order_time DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rs.next()) {
                    orders.add(mapOrder(rs));
                }
                return orders;
            }
        }
    }

    public List<Order> getAllOrders() throws SQLException {
        ensureCustomerOrderColumns();
        String sql = """
                SELECT o.*, t.table_name, u.full_name AS user_name,
                       c.full_name AS customer_name, c.phone AS customer_phone,
                       c.email AS customer_email, c.address AS customer_address,
                       p.method AS payment_method, p.amount_paid, p.payment_time
                FROM Orders o
                LEFT JOIN TableCafe t ON o.table_id = t.table_id
                LEFT JOIN [User] u ON o.user_id = u.user_id
                LEFT JOIN Customer c ON o.customer_id = c.customer_id
                LEFT JOIN Payment p ON o.order_id = p.order_id
                WHERE o.is_deleted = 0
                ORDER BY o.order_time DESC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Order> orders = new ArrayList<>();
            while (rs.next()) {
                Order order = mapOrder(rs);
                order.setDetails(getOrderDetails(order.getOrderId()));
                orders.add(order);
            }
            return orders;
        }
    }

    public List<Order> searchOrders(String keyword) throws SQLException {
        ensureCustomerOrderColumns();
        String sql = """
                SELECT o.*, t.table_name, u.full_name AS user_name,
                       c.full_name AS customer_name, c.phone AS customer_phone,
                       c.email AS customer_email, c.address AS customer_address,
                       p.method AS payment_method, p.amount_paid, p.payment_time
                FROM Orders o
                LEFT JOIN TableCafe t ON o.table_id = t.table_id
                LEFT JOIN [User] u ON o.user_id = u.user_id
                LEFT JOIN Customer c ON o.customer_id = c.customer_id
                LEFT JOIN Payment p ON o.order_id = p.order_id
                WHERE o.is_deleted = 0
                  AND (CONVERT(VARCHAR(20), o.order_id) LIKE ?
                   OR ISNULL(c.full_name, '') LIKE ?
                   OR ISNULL(c.phone, '') LIKE ?
                   OR ISNULL(c.email, '') LIKE ?
                   OR ISNULL(t.table_name, '') LIKE ?
                   OR ISNULL(u.full_name, '') LIKE ?
                   OR ISNULL(o.status, '') LIKE ?
                   OR ISNULL(o.order_type, '') LIKE ?
                   OR ISNULL(p.method, '') LIKE ?)
                ORDER BY o.order_time DESC
                """;
        String pattern = "%" + keyword + "%";
        String idPattern = "%" + keyword.replace("#", "").trim() + "%";
        String statusPattern = resolveStatusPattern(keyword, pattern);
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, idPattern);
            for (int index = 2; index <= 6; index++) {
                stmt.setString(index, pattern);
            }
            stmt.setString(7, statusPattern);
            stmt.setString(8, pattern);
            stmt.setString(9, pattern);
            try (ResultSet rs = stmt.executeQuery()) {
                List<Order> orders = new ArrayList<>();
                while (rs.next()) {
                    Order order = mapOrder(rs);
                    order.setDetails(getOrderDetails(order.getOrderId()));
                    orders.add(order);
                }
                return orders;
            }
        }
    }

    private String resolveStatusPattern(String keyword, String fallbackPattern) {
        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("đã thanh toán") || normalized.contains("da thanh toan")
                || normalized.equals("paid")) {
            return "%paid%";
        }
        if (normalized.contains("đã hủy") || normalized.contains("da huy")
                || normalized.contains("cancel")) {
            return "%cancelled%";
        }
        if (normalized.contains("đang chờ") || normalized.contains("dang cho")
                || normalized.equals("pending")) {
            return "%pending%";
        }
        return fallbackPattern;
    }

    public List<Order> getPendingCustomerOrders() throws SQLException {
        ensureCustomerOrderColumns();
        String sql = """
                SELECT o.*, t.table_name, u.full_name AS user_name,
                       c.full_name AS customer_name, c.phone AS customer_phone,
                       c.email AS customer_email, c.address AS customer_address,
                       p.method AS payment_method, p.amount_paid, p.payment_time
                FROM Orders o
                LEFT JOIN TableCafe t ON o.table_id = t.table_id
                LEFT JOIN [User] u ON o.user_id = u.user_id
                JOIN Customer c ON o.customer_id = c.customer_id
                LEFT JOIN Payment p ON o.order_id = p.order_id
                WHERE o.status = 'pending'
                  AND o.is_deleted = 0
                  AND o.requested_payment_method IS NOT NULL
                  AND LTRIM(RTRIM(o.requested_payment_method)) <> ''
                ORDER BY o.order_time ASC
                """;
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            List<Order> orders = new ArrayList<>();
            while (rs.next()) {
                Order order = mapOrder(rs);
                order.setDetails(getOrderDetails(order.getOrderId()));
                orders.add(order);
            }
            return orders;
        }
    }

    public int countProducts() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) AS total FROM Product WHERE is_available = 1")) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    public boolean softDelete(int orderId) throws SQLException {
        ensureCustomerOrderColumns();
        String selectSql = "SELECT table_id FROM Orders WHERE order_id = ? AND is_deleted = 0";
        try (Connection conn = DatabaseConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Integer tableId = null;
                try (PreparedStatement stmt = conn.prepareStatement(selectSql)) {
                    stmt.setInt(1, orderId);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            return false;
                        }
                        int value = rs.getInt("table_id");
                        if (!rs.wasNull()) {
                            tableId = value;
                        }
                    }
                }
                try (PreparedStatement stmt = conn.prepareStatement("UPDATE Orders SET is_deleted = 1 WHERE order_id = ? AND is_deleted = 0")) {
                    stmt.setInt(1, orderId);
                    if (stmt.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
                if (tableId != null) {
                    try (PreparedStatement stmt = conn.prepareStatement("UPDATE TableCafe SET status = 'empty' WHERE table_id = ?")) {
                        stmt.setInt(1, tableId);
                        stmt.executeUpdate();
                    }
                }
                conn.commit();
                return true;
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    private void applyAmountPercent(List<StatisticItem> items) {
        double max = items.stream().mapToDouble(StatisticItem::getAmount).max().orElse(0);
        for (StatisticItem item : items) {
            item.setPercent(max <= 0 ? 0 : item.getAmount() * 100 / max);
        }
    }

    private void applyCountPercent(List<StatisticItem> items) {
        int max = items.stream().mapToInt(StatisticItem::getCount).max().orElse(0);
        for (StatisticItem item : items) {
            item.setPercent(max <= 0 ? 0 : item.getCount() * 100.0 / max);
        }
    }

    private void applyProductSalesPercent(List<ProductSalesItem> items) {
        int max = items.stream().mapToInt(ProductSalesItem::getQuantity).max().orElse(0);
        for (ProductSalesItem item : items) {
            item.setPercent(max <= 0 ? 0 : item.getQuantity() * 100.0 / max);
        }
    }

    private void ensureProductImageColumn() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COL_LENGTH(N'dbo.Product', N'image_url') AS image_column")) {
            if (!rs.next() || rs.getObject("image_column") == null) {
                throw new SQLException("Database chưa có cột ảnh sản phẩm. Vui lòng chạy src/main/resources/db/otp-email-migration.sql.");
            }
        }
    }

    private void ensureCustomerOrderColumns() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT COL_LENGTH(N'dbo.Orders', N'customer_id') AS customer_column,
                            COL_LENGTH(N'dbo.Orders', N'order_type') AS type_column,
                            COL_LENGTH(N'dbo.Orders', N'delivery_address') AS address_column,
                            COL_LENGTH(N'dbo.Orders', N'customer_note') AS note_column,
                            COL_LENGTH(N'dbo.Orders', N'requested_payment_method') AS payment_column,
                            COL_LENGTH(N'dbo.Orders', N'is_deleted') AS deleted_column
                     """)) {
            if (!rs.next()
                    || rs.getObject("customer_column") == null
                    || rs.getObject("type_column") == null
                    || rs.getObject("address_column") == null
                    || rs.getObject("note_column") == null
                    || rs.getObject("payment_column") == null
                    || rs.getObject("deleted_column") == null) {
                throw new SQLException(ORDER_MIGRATION_MESSAGE);
            }
        }
    }

    private void ensureOrderCancellationSupport() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("""
                     SELECT cc.definition
                     FROM sys.check_constraints cc
                     WHERE cc.parent_object_id = OBJECT_ID(N'dbo.Orders')
                       AND cc.definition LIKE '%status%'
                     """)) {
            boolean hasCancellationConstraint = false;
            while (rs.next()) {
                String definition = rs.getString("definition");
                if (definition != null && definition.contains("cancelled")) {
                    hasCancellationConstraint = true;
                }
            }
            if (!hasCancellationConstraint) {
                throw new SQLException(ORDER_MIGRATION_MESSAGE);
            }
        }
    }

    private void insertOrderDetail(Connection conn, int orderId, int productId, int quantity) throws SQLException {
        String sql = """
                INSERT INTO OrderDetail(order_id, product_id, quantity, price)
                SELECT ?, product_id, ?, price
                FROM Product
                WHERE product_id = ? AND is_available = 1
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, orderId);
            stmt.setInt(2, quantity);
            stmt.setInt(3, productId);
            if (stmt.executeUpdate() == 0) {
                throw new SQLException("Sản phẩm không tồn tại hoặc đã ngừng bán.");
            }
        }
    }

    private Order mapOrder(ResultSet rs) throws SQLException {
        Order order = new Order();
        order.setOrderId(rs.getInt("order_id"));
        order.setTableId(rs.getInt("table_id"));
        order.setTableName(rs.getString("table_name"));
        order.setUserId(rs.getInt("user_id"));
        order.setUserName(rs.getString("user_name"));
        order.setCustomerId(rs.getInt("customer_id"));
        order.setCustomerName(rs.getString("customer_name"));
        order.setCustomerPhone(rs.getString("customer_phone"));
        order.setCustomerEmail(rs.getString("customer_email"));
        order.setCustomerAddress(rs.getString("customer_address"));
        order.setOrderTime(String.valueOf(rs.getTimestamp("order_time")));
        order.setStatus(rs.getString("status"));
        order.setTotal(rs.getDouble("total"));
        order.setOrderType(rs.getString("order_type"));
        order.setDeliveryAddress(rs.getString("delivery_address"));
        order.setCustomerNote(rs.getString("customer_note"));
        order.setRequestedPaymentMethod(rs.getString("requested_payment_method"));
        order.setPaymentMethod(rs.getString("payment_method"));
        order.setAmountPaid(rs.getDouble("amount_paid"));
        order.setPaymentTime(String.valueOf(rs.getTimestamp("payment_time")));
        return order;
    }
}
