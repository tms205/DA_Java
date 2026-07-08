package com.huit.da_java.controller;

import com.huit.da_java.dao.CustomerDAO;
import com.huit.da_java.dao.FeedbackDAO;
import com.huit.da_java.dao.OrderDAO;
import com.huit.da_java.dao.ProductDAO;
import com.huit.da_java.dao.UserDAO;
import com.huit.da_java.model.Customer;
import com.huit.da_java.model.Order;
import com.huit.da_java.model.Product;
import com.huit.da_java.model.StatisticItem;
import com.huit.da_java.model.User;
import com.huit.da_java.service.OrderNotificationService;
import com.huit.da_java.service.ProductImageStorageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
public class AdminController {
    private final ProductDAO productDAO;
    private final UserDAO userDAO;
    private final OrderDAO orderDAO;
    private final CustomerDAO customerDAO;
    private final FeedbackDAO feedbackDAO;
    private final OrderNotificationService orderNotificationService;
    private final ProductImageStorageService productImageStorageService;

    public AdminController(ProductDAO productDAO,
                           UserDAO userDAO,
                           OrderDAO orderDAO,
                           CustomerDAO customerDAO,
                           FeedbackDAO feedbackDAO,
                           OrderNotificationService orderNotificationService,
                           ProductImageStorageService productImageStorageService) {
        this.productDAO = productDAO;
        this.userDAO = userDAO;
        this.orderDAO = orderDAO;
        this.customerDAO = customerDAO;
        this.feedbackDAO = feedbackDAO;
        this.orderNotificationService = orderNotificationService;
        this.productImageStorageService = productImageStorageService;
    }

    @GetMapping("/admin")
    public String overview(HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        addBaseModel(session, model, "overview");
        try {
            int paidOrders = orderDAO.getPaidOrderCount();
            double totalRevenue = orderDAO.getTotalRevenue();
            model.addAttribute("todayRevenue", orderDAO.getRevenueByDate(LocalDate.now()));
            model.addAttribute("todayOrders", orderDAO.getPaidOrderCountByDate(LocalDate.now()));
            model.addAttribute("totalRevenue", totalRevenue);
            model.addAttribute("paidOrders", paidOrders);
            model.addAttribute("averageOrderValue", paidOrders == 0 ? 0 : totalRevenue / paidOrders);
            model.addAttribute("pendingOrdersCount", orderDAO.getPendingCustomerOrderCount());
            model.addAttribute("recentOrders", orderDAO.getRecentActiveOrders(8));
        } catch (Exception ex) {
            addLoadError(model, ex);
        }
        return "admin";
    }

    @GetMapping("/admin/dashboard")
    public String dashboard(HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        addBaseModel(session, model, "dashboard");
        try {
            model.addAttribute("categoryRevenueShares", orderDAO.getCategoryRevenueShares());
            model.addAttribute("revenueChart", orderDAO.getRevenueLastDays(7));
            model.addAttribute("paymentStats", orderDAO.getPaymentMethodStats());
            model.addAttribute("topProducts", orderDAO.getTopProductSales(3));
        } catch (Exception ex) {
            addLoadError(model, ex);
            model.addAttribute("categoryRevenueShares", List.of());
            model.addAttribute("revenueChart", List.of());
            model.addAttribute("paymentStats", List.of());
            model.addAttribute("topProducts", List.of());
        }
        return "admin-dashboard";
    }

    @GetMapping("/admin/products")
    public String products(@RequestParam(required = false) String productKeyword,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        addBaseModel(session, model, "products");
        try {
            List<Product> products = isBlank(productKeyword) ? productDAO.getAll() : productDAO.search(productKeyword);
            List<StatisticItem> categoryStats = productDAO.getProductCountByCategory();
            model.addAttribute("products", products);
            model.addAttribute("categories", productDAO.getCategories());
            model.addAttribute("categoryStats", categoryStats);
            model.addAttribute("categoryCount", categoryStats.size());
            model.addAttribute("productCount", products.size());
            model.addAttribute("availableProductCount", productDAO.countAvailableProducts());
            model.addAttribute("hiddenProductCount", productDAO.countHiddenProducts());
            model.addAttribute("topProducts", orderDAO.getTopProductSales(3));
        } catch (Exception ex) {
            addLoadError(model, ex);
            model.addAttribute("products", List.of());
            model.addAttribute("categories", List.of());
            model.addAttribute("categoryStats", List.of());
            model.addAttribute("topProducts", List.of());
        }
        model.addAttribute("product", new Product());
        return "admin-products";
    }

    @GetMapping("/admin/staff")
    public String staff(@RequestParam(required = false) String staffKeyword,
                        HttpSession session,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        addBaseModel(session, model, "staff");
        try {
            List<User> staff = isBlank(staffKeyword) ? userDAO.getAllStaff() : userDAO.searchStaffByName(staffKeyword);
            List<StatisticItem> staffRevenueStats = userDAO.getStaffRevenueStats();
            model.addAttribute("staff", staff);
            model.addAttribute("staffCount", staff.size());
            model.addAttribute("activeStaffCount", userDAO.countActiveStaff());
            model.addAttribute("inactiveStaffCount", userDAO.countInactiveStaff());
            model.addAttribute("staffRevenueStats", staffRevenueStats);
            model.addAttribute("staffHandledOrderCount", staffRevenueStats.stream().mapToInt(StatisticItem::getCount).sum());
        } catch (Exception ex) {
            addLoadError(model, ex);
            model.addAttribute("staff", List.of());
            model.addAttribute("staffRevenueStats", List.of());
        }
        model.addAttribute("newStaff", new User());
        return "admin-staff";
    }

    @GetMapping("/admin/customers")
    public String customers(@RequestParam(required = false) String customerKeyword,
                            HttpSession session,
                            Model model,
                            RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        addBaseModel(session, model, "customers");
        try {
            List<Customer> customers = isBlank(customerKeyword)
                    ? customerDAO.getAll()
                    : customerDAO.search(customerKeyword.trim());
            model.addAttribute("customers", customers);
            model.addAttribute("customerCount", customers.size());
            model.addAttribute("activeCustomerCount", customerDAO.countActiveCustomers());
            model.addAttribute("inactiveCustomerCount", customerDAO.countInactiveCustomers());
            model.addAttribute("customerKeyword", customerKeyword);
        } catch (Exception ex) {
            addLoadError(model, ex);
            model.addAttribute("customers", List.of());
        }
        return "admin-customers";
    }

    @GetMapping("/admin/orders")
    public String pendingOrders(@RequestParam(required = false) String orderKeyword,
                                HttpSession session,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        addBaseModel(session, model, "orders");
        try {
            List<Order> pendingOrders = orderDAO.getPendingCustomerOrders();
            List<Order> recentOrders = orderDAO.getRecentOrders(12);
            List<Order> allOrders = isBlank(orderKeyword)
                    ? orderDAO.getAllOrders()
                    : orderDAO.searchOrders(orderKeyword.trim());
            List<?> paymentStats = orderDAO.getPaymentMethodStats();
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("pendingOrdersCount", orderDAO.getPendingCustomerOrderCount());
            model.addAttribute("recentOrders", recentOrders);
            model.addAttribute("allOrders", allOrders);
            model.addAttribute("orderKeyword", orderKeyword);
            model.addAttribute("paymentStats", paymentStats);
            model.addAttribute("recentOrderCount", recentOrders.size());
            model.addAttribute("allOrderCount", allOrders.size());
            model.addAttribute("paymentMethodCount", paymentStats.size());
        } catch (Exception ex) {
            addLoadError(model, ex);
            model.addAttribute("pendingOrders", List.of());
            model.addAttribute("recentOrders", List.of());
            model.addAttribute("allOrders", List.of());
            model.addAttribute("paymentStats", List.of());
        }
        return "admin-orders";
    }

    @GetMapping("/admin/feedback")
    public String feedback(HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        addBaseModel(session, model, "feedback");
        try {
            model.addAttribute("feedbackList", feedbackDAO.getRecent(80));
            model.addAttribute("feedbackCount", feedbackDAO.countAll());
        } catch (Exception ex) {
            addLoadError(model, ex);
            model.addAttribute("feedbackList", List.of());
            model.addAttribute("feedbackCount", 0);
        }
        return "admin-feedback";
    }

    @PostMapping("/admin/products")
    public String saveProduct(@ModelAttribute Product product,
                              @RequestParam(name = "imageFile", required = false) MultipartFile imageFile,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            String uploadedImageUrl = productImageStorageService.store(imageFile);
            if (uploadedImageUrl != null) {
                product.setImageUrl(uploadedImageUrl);
            }
            if (product.getProductId() > 0) {
                productDAO.update(product);
                redirectAttributes.addFlashAttribute("success", "Đã cập nhật sản phẩm.");
            } else {
                product.setAvailable(true);
                productDAO.add(product);
                redirectAttributes.addFlashAttribute("success", "Đã thêm sản phẩm mới.");
            }
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Lưu sản phẩm thất bại: " + ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/products/{productId}/delete")
    public String deleteProduct(@PathVariable int productId,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            productDAO.delete(productId);
            redirectAttributes.addFlashAttribute("success", "Đã ẩn sản phẩm khỏi menu.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể xóa sản phẩm: " + ex.getMessage());
        }
        return "redirect:/admin/products";
    }

    @PostMapping("/admin/orders/{orderId}/cancel")
    public String cancelOrder(@PathVariable int orderId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            orderDAO.cancelOrder(orderId);
            orderNotificationService.notifyOrderUpdated(orderId, "cancelled");
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn #" + orderId + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể hủy đơn: " + ex.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/orders/{orderId}/delete")
    public String deleteOrder(@PathVariable int orderId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            if (!orderDAO.softDelete(orderId)) {
                throw new IllegalArgumentException("Không tìm thấy đơn hàng.");
            }
            orderNotificationService.notifyOrderUpdated(orderId, "removed");
            redirectAttributes.addFlashAttribute("success", "Đã ẩn đơn #" + orderId + " khỏi phần mềm. Dữ liệu vẫn được lưu trong database.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể ẩn đơn hàng: " + ex.getMessage());
        }
        return "redirect:/admin/orders";
    }

    @PostMapping("/admin/customers")
    public String saveCustomer(@ModelAttribute Customer customer,
                               HttpSession session,
                               RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            if (isBlank(customer.getPhone()) || isBlank(customer.getFullName())) {
                throw new IllegalArgumentException("Số điện thoại và họ tên khách hàng là bắt buộc.");
            }
            if (customer.getCustomerId() <= 0) {
                throw new IllegalArgumentException("Khách hàng mới cần tự đăng ký và xác thực OTP.");
            }
            customerDAO.update(customer);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật tài khoản khách hàng.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Lưu tài khoản khách hàng thất bại: " + ex.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/admin/customers/{customerId}/toggle")
    public String toggleCustomer(@PathVariable int customerId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            customerDAO.toggleActive(customerId);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái tài khoản khách hàng.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật khách hàng: " + ex.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/admin/customers/{customerId}/delete")
    public String deleteCustomer(@PathVariable int customerId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            if (!customerDAO.softDelete(customerId)) {
                throw new IllegalArgumentException("Không tìm thấy khách hàng.");
            }
            redirectAttributes.addFlashAttribute("success", "Đã ẩn khách hàng khỏi phần mềm. Dữ liệu vẫn được lưu trong database.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể ẩn khách hàng: " + ex.getMessage());
        }
        return "redirect:/admin/customers";
    }

    @PostMapping("/admin/staff")
    public String saveStaff(@ModelAttribute User user,
                            HttpSession session,
                            RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        if (isBlank(user.getRole())) {
            user.setRole("staff");
        }
        try {
            if (isBlank(user.getEmail()) || !user.getEmail().contains("@")) {
                throw new IllegalArgumentException("Email nhân viên là bắt buộc để sử dụng khôi phục mật khẩu.");
            }
            userDAO.registerStaff(user);
            redirectAttributes.addFlashAttribute("success", "Đã tạo tài khoản nhân viên.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Tạo nhân viên thất bại: " + ex.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{userId}/toggle")
    public String toggleStaff(@PathVariable int userId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            userDAO.toggleActive(userId);
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái nhân viên.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật nhân viên: " + ex.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{userId}/delete")
    public String deleteStaff(@PathVariable int userId,
                              HttpSession session,
                              RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            if (session.getAttribute("userId") instanceof Integer currentUserId && currentUserId == userId) {
                throw new IllegalArgumentException("Không thể xóa tài khoản đang đăng nhập.");
            }
            if (!userDAO.softDelete(userId)) {
                throw new IllegalArgumentException("Chỉ có thể xóa tài khoản nhân viên đang tồn tại.");
            }
            redirectAttributes.addFlashAttribute("success", "Đã ẩn nhân viên khỏi phần mềm. Dữ liệu vẫn được lưu trong database.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể ẩn nhân viên: " + ex.getMessage());
        }
        return "redirect:/admin/staff";
    }

    @PostMapping("/admin/staff/{userId}/email")
    public String updateStaffRecoveryEmail(@PathVariable int userId,
                                           @RequestParam String email,
                                           HttpSession session,
                                           RedirectAttributes redirectAttributes) {
        if (!requireAdmin(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            if (isBlank(email) || !email.contains("@")) {
                throw new IllegalArgumentException("Vui lòng nhập email khôi phục hợp lệ.");
            }
            if (!userDAO.updateRecoveryEmail(userId, email.trim().toLowerCase())) {
                throw new IllegalArgumentException("Không tìm thấy tài khoản nhân viên.");
            }
            redirectAttributes.addFlashAttribute("success", "Đã cập nhật email khôi phục.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Cập nhật email thất bại: " + ex.getMessage());
        }
        return "redirect:/admin/staff";
    }

    private void addBaseModel(HttpSession session, Model model, String activePage) {
        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("activePage", activePage);
    }

    private void addLoadError(Model model, Exception ex) {
        model.addAttribute("loadError", ex.getMessage());
    }

    private boolean requireAdmin(HttpSession session, RedirectAttributes redirectAttributes) {
        if ("admin".equals(session.getAttribute("role"))) {
            return true;
        }
        redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập bằng tài khoản admin.");
        return false;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
