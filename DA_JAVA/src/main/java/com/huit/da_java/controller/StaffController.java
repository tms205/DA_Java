package com.huit.da_java.controller;

import com.huit.da_java.dao.OrderDAO;
import com.huit.da_java.model.Order;
import com.huit.da_java.service.OrderNotificationService;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class StaffController {
    private final OrderDAO orderDAO;
    private final OrderNotificationService orderNotificationService;

    public StaffController(OrderDAO orderDAO, OrderNotificationService orderNotificationService) {
        this.orderDAO = orderDAO;
        this.orderNotificationService = orderNotificationService;
    }

    @GetMapping("/staff/pos")
    public String pos(HttpSession session,
                      Model model,
                      RedirectAttributes redirectAttributes) {
        if (!requireStaff(session, redirectAttributes)) {
            return "redirect:/login";
        }
        try {
            List<Order> pendingOrders = orderDAO.getPendingCustomerOrders();
            double pendingTotal = pendingOrders.stream().mapToDouble(Order::getTotal).sum();
            int pendingItems = pendingOrders.stream()
                    .mapToInt(order -> order.getDetails().stream().mapToInt(detail -> detail.getQuantity()).sum())
                    .sum();
            model.addAttribute("pendingOrders", pendingOrders);
            model.addAttribute("pendingOrdersCount", pendingOrders.size());
            model.addAttribute("pendingItemsCount", pendingItems);
            model.addAttribute("pendingTotalFormatted", String.format("%,.0f VND", pendingTotal));
        } catch (Exception ex) {
            model.addAttribute("loadError", ex.getMessage());
            model.addAttribute("pendingOrders", java.util.List.of());
            model.addAttribute("pendingOrdersCount", 0);
            model.addAttribute("pendingItemsCount", 0);
            model.addAttribute("pendingTotalFormatted", "0 VND");
        }
        model.addAttribute("fullName", session.getAttribute("fullName"));
        return "staff-pos";
    }

    @GetMapping(value = "/staff/orders/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter orderStream(HttpSession session) {
        Object role = session.getAttribute("role");
        if (!"staff".equals(role) && !"admin".equals(role)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return orderNotificationService.subscribe();
    }

    @PostMapping("/staff/checkout")
    public String checkout(@RequestParam int orderId,
                           @RequestParam(defaultValue = "cash") String method,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (!"staff".equals(session.getAttribute("role")) && !"admin".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/login";
        }
        try {
            int userId = (int) session.getAttribute("userId");
            orderDAO.payPendingCustomerOrder(orderId, userId, method);
            orderNotificationService.notifyOrderUpdated(orderId, "paid");
            redirectAttributes.addFlashAttribute("success", "Đã thanh toán đơn #" + orderId + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Thanh toán thất bại: " + ex.getMessage());
        }
        return "redirect:/staff/pos";
    }

    @PostMapping("/staff/cancel")
    public String cancel(@RequestParam int orderId,
                         HttpSession session,
                         RedirectAttributes redirectAttributes) {
        if (!"staff".equals(session.getAttribute("role")) && !"admin".equals(session.getAttribute("role"))) {
            redirectAttributes.addFlashAttribute("error", "Phiên đăng nhập đã hết hạn.");
            return "redirect:/login";
        }
        try {
            orderDAO.cancelOrder(orderId);
            orderNotificationService.notifyOrderUpdated(orderId, "cancelled");
            redirectAttributes.addFlashAttribute("success", "Đã hủy đơn #" + orderId + ".");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Hủy đơn thất bại: " + ex.getMessage());
        }
        return "redirect:/staff/pos";
    }

    private boolean requireStaff(HttpSession session, RedirectAttributes redirectAttributes) {
        Object role = session.getAttribute("role");
        if ("staff".equals(role) || "admin".equals(role)) {
            return true;
        }
        redirectAttributes.addFlashAttribute("error", "Bạn cần đăng nhập bằng tài khoản nhân viên.");
        return false;
    }
}
