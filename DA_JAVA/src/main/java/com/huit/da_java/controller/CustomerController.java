package com.huit.da_java.controller;

import com.huit.da_java.dao.OrderDAO;
import com.huit.da_java.dao.ProductDAO;
import com.huit.da_java.model.Order;
import com.huit.da_java.model.OrderDetail;
import com.huit.da_java.model.Product;
import com.huit.da_java.service.OrderNotification;
import com.huit.da_java.service.OrderNotificationService;
import com.huit.da_java.service.QrCodeService;
import com.huit.da_java.service.VietQrPayloadService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
public class CustomerController {
    private static final String MOMO_WALLET_PHONE = "0769479813";

    private final ProductDAO productDAO;
    private final OrderDAO orderDAO;
    private final OrderNotificationService orderNotificationService;
    private final QrCodeService qrCodeService;
    private final VietQrPayloadService vietQrPayloadService;

    public CustomerController(ProductDAO productDAO,
                              OrderDAO orderDAO,
                              OrderNotificationService orderNotificationService,
                              QrCodeService qrCodeService,
                              VietQrPayloadService vietQrPayloadService) {
        this.productDAO = productDAO;
        this.orderDAO = orderDAO;
        this.orderNotificationService = orderNotificationService;
        this.qrCodeService = qrCodeService;
        this.vietQrPayloadService = vietQrPayloadService;
    }

    @GetMapping("/customer/menu")
    public String menu(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) Integer categoryId,
                       HttpSession session,
                       Model model,
                       RedirectAttributes redirectAttributes) {
        if (!"customer".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập bằng tài khoản khách hàng.");
            return "redirect:/login/customer";
        }
        try {
            model.addAttribute("products", findProducts(keyword, categoryId));
            model.addAttribute("categories", productDAO.getCategories());
        } catch (Exception ex) {
            model.addAttribute("loadError", ex.getMessage());
            model.addAttribute("products", List.of());
            model.addAttribute("categories", List.of());
        }
        model.addAttribute("fullName", session.getAttribute("fullName"));
        model.addAttribute("customerAddress", session.getAttribute("customerAddress"));
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedCategoryId", categoryId);
        return "customer-menu";
    }

    @PostMapping("/customer/order")
    public String order(@RequestParam(required = false) List<Integer> productId,
                        @RequestParam(required = false) List<Integer> quantity,
                        @RequestParam(defaultValue = "takeaway") String orderType,
                        @RequestParam(required = false) String paymentMethod,
                        @RequestParam(required = false) String deliveryAddress,
                        @RequestParam(required = false) String note,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (!"customer".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập bằng tài khoản khách hàng.");
            return "redirect:/login/customer";
        }
        try {
            List<Product> products = productDAO.getAvailable();
            Map<Integer, Product> productMap = products.stream()
                    .collect(Collectors.toMap(Product::getProductId, product -> product));
            List<OrderDetail> items = buildItems(productId, quantity, productMap);
            if (items.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Vui lòng chọn ít nhất một món.");
                return "redirect:/customer/menu";
            }

            int customerId = (int) session.getAttribute("customerId");
            String normalizedOrderType = normalizeOrderType(orderType);
            String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
            String normalizedDeliveryAddress = normalizeDeliveryAddress(
                    normalizedOrderType,
                    normalizedPaymentMethod,
                    deliveryAddress);
            int orderId = orderDAO.createCustomerOrder(
                    customerId,
                    items,
                    note,
                    normalizedOrderType,
                    normalizedDeliveryAddress,
                    normalizedPaymentMethod);
            redirectAttributes.addFlashAttribute("success",
                    "Đã tạo giỏ hàng #" + orderId + ". Vui lòng chọn phương thức thanh toán.");
            return "redirect:/customer/payment/" + orderId;
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể đặt món: " + ex.getMessage());
            return "redirect:/customer/menu";
        }
    }

    @GetMapping("/customer/payment/{orderId}")
    public String payment(@PathVariable int orderId,
                          HttpSession session,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        if (!"customer".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập bằng tài khoản khách hàng.");
            return "redirect:/login/customer";
        }
        try {
            Order order = getCurrentCustomerOrder(orderId, session);
            model.addAttribute("order", order);
            model.addAttribute("fullName", session.getAttribute("fullName"));
            model.addAttribute("customerAddress", session.getAttribute("customerAddress"));
            model.addAttribute("paymentContent", buildPaymentContent(order));
            model.addAttribute("momoWalletPhone", MOMO_WALLET_PHONE);
            boolean paymentSelected = hasRequestedPaymentMethod(order);
            model.addAttribute("paymentSelected", paymentSelected);
            model.addAttribute("qrPayment", paymentSelected && order.isQrPayment());
            return "customer-payment";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể mở thanh toán: " + ex.getMessage());
            return "redirect:/customer/menu";
        }
    }

    @PostMapping("/customer/payment/{orderId}/method")
    public String choosePaymentMethod(@PathVariable int orderId,
                                      @RequestParam String paymentMethod,
                                      @RequestParam(defaultValue = "takeaway") String orderType,
                                      @RequestParam(required = false) String deliveryAddress,
                                      HttpSession session,
                                      RedirectAttributes redirectAttributes) {
        if (!"customer".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập bằng tài khoản khách hàng.");
            return "redirect:/login/customer";
        }
        try {
            Order order = getCurrentCustomerOrder(orderId, session);
            boolean submittedBefore = hasRequestedPaymentMethod(order);
            String normalizedOrderType = normalizeOrderType(orderType);
            String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
            if (normalizedPaymentMethod == null) {
                throw new IllegalArgumentException("Vui lòng chọn phương thức thanh toán.");
            }
            String normalizedDeliveryAddress = normalizeDeliveryAddress(
                    normalizedOrderType,
                    normalizedPaymentMethod,
                    deliveryAddress);
            orderDAO.updateCustomerCheckoutOptions(
                    orderId,
                    (int) session.getAttribute("customerId"),
                    normalizedOrderType,
                    normalizedDeliveryAddress,
                    normalizedPaymentMethod);
            if (!submittedBefore) {
                orderNotificationService.notifyNewOrder(new OrderNotification(
                        order.getOrderId(),
                        String.valueOf(session.getAttribute("fullName")),
                        order.getTotalFormatted(),
                        order.getCustomerNote(),
                        LocalDateTime.now()));
            }
            redirectAttributes.addFlashAttribute("success",
                    "Đã chọn phương thức thanh toán cho đơn #" + orderId + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể chọn phương thức thanh toán: " + ex.getMessage());
        }
        return "redirect:/customer/payment/" + orderId;
    }

    @GetMapping(value = "/customer/payment/{orderId}/qr", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> paymentQr(@PathVariable int orderId,
                                            HttpSession session) throws Exception {
        if (!"customer".equals(session.getAttribute("role"))) {
            return ResponseEntity.status(401).build();
        }
        Order order = getCurrentCustomerOrder(orderId, session);
        if (!order.isQrPayment()) {
            return ResponseEntity.badRequest().build();
        }
        byte[] image = qrCodeService.generatePng(buildQrPayload(order), 360);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(image);
    }

    @PostMapping("/customer/payment/{orderId}/confirm")
    public String confirmPayment(@PathVariable int orderId,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {
        if (!"customer".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập bằng tài khoản khách hàng.");
            return "redirect:/login/customer";
        }
        try {
            Order order = getCurrentCustomerOrder(orderId, session);
            redirectAttributes.addFlashAttribute("success",
                    "Đã ghi nhận bạn đã chuyển khoản cho đơn #" + order.getOrderId()
                            + ". Nhân viên sẽ kiểm tra và xác nhận thanh toán tại quầy.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể xác nhận thanh toán: " + ex.getMessage());
        }
        return "redirect:/customer/menu";
    }

    private List<OrderDetail> buildItems(List<Integer> productIds,
                                         List<Integer> quantities,
                                         Map<Integer, Product> productMap) {
        List<OrderDetail> items = new ArrayList<>();
        if (productIds == null || quantities == null) {
            return items;
        }
        int size = Math.min(productIds.size(), quantities.size());
        for (int i = 0; i < size; i++) {
            int qty = quantities.get(i);
            Product product = productMap.get(productIds.get(i));
            if (qty > 0 && product != null) {
                items.add(new OrderDetail(product.getProductId(), product.getName(), qty, product.getPrice()));
            }
        }
        return items;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private Order getCurrentCustomerOrder(int orderId, HttpSession session) throws Exception {
        int customerId = (int) session.getAttribute("customerId");
        Order order = orderDAO.getOrderById(orderId);
        if (order == null || order.getCustomerId() != customerId) {
            throw new IllegalArgumentException("Đơn hàng không tồn tại hoặc không thuộc tài khoản của bạn.");
        }
        return order;
    }

    private String buildPaymentContent(Order order) {
        return "DON" + order.getOrderId();
    }

    private String normalizeOrderType(String orderType) {
        if ("delivery".equals(orderType)) {
            return "delivery";
        }
        return "takeaway";
    }

    private String normalizePaymentMethod(String paymentMethod) {
        if (paymentMethod == null || paymentMethod.isBlank()) {
            return null;
        }
        if ("cash".equals(paymentMethod)) {
            return "cash";
        }
        if ("bank".equals(paymentMethod)) {
            return "bank";
        }
        return "momo";
    }

    private String normalizeDeliveryAddress(String orderType, String paymentMethod, String deliveryAddress) {
        if (!"delivery".equals(orderType)) {
            return null;
        }
        if ("cash".equals(paymentMethod)) {
            throw new IllegalArgumentException("Đơn giao tận nhà cần thanh toán trước bằng QR.");
        }
        if (deliveryAddress == null || deliveryAddress.trim().length() < 10) {
            throw new IllegalArgumentException("Vui lòng nhập địa chỉ giao hàng chi tiết.");
        }
        return deliveryAddress.trim();
    }

    private String buildQrPayload(Order order) {
        return vietQrPayloadService.buildMomoTransferPayload(
                Math.round(order.getTotal()),
                buildPaymentContent(order));
    }

    private boolean hasRequestedPaymentMethod(Order order) {
        String method = order.getRequestedPaymentMethod();
        return method != null && !method.isBlank();
    }

    private List<Product> findProducts(String keyword, Integer categoryId) throws Exception {
        boolean hasKeyword = !isBlank(keyword);
        boolean hasCategory = categoryId != null && categoryId > 0;
        if (hasKeyword && hasCategory) {
            return productDAO.searchAvailableByCategory(keyword, categoryId);
        }
        if (hasCategory) {
            return productDAO.getAvailableByCategory(categoryId);
        }
        if (hasKeyword) {
            return productDAO.searchAvailable(keyword);
        }
        return productDAO.getAvailable();
    }
}
