package com.huit.da_java.controller;

import com.huit.da_java.dao.OrderDAO;
import com.huit.da_java.dao.FeedbackDAO;
import com.huit.da_java.dao.ProductDAO;
import com.huit.da_java.model.Order;
import com.huit.da_java.model.Product;
import com.huit.da_java.service.OrderNotification;
import com.huit.da_java.service.OrderNotificationService;
import com.huit.da_java.service.QrCodeService;
import com.huit.da_java.service.VietQrPayloadService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CustomerControllerNotificationTest {

    @Test
    void creatingCartDraftDoesNotNotifyStaff() throws Exception {
        ProductDAO productDAO = mock(ProductDAO.class);
        OrderDAO orderDAO = mock(OrderDAO.class);
        OrderNotificationService notificationService = mock(OrderNotificationService.class);
        CustomerController controller = createController(productDAO, orderDAO, notificationService);
        Product product = new Product("Americano", 40000, 1);
        product.setProductId(8);
        when(productDAO.getAvailable()).thenReturn(List.of(product));
        when(orderDAO.createCustomerOrder(anyInt(), anyList(), any(), any(), any(), any()))
                .thenReturn(31);

        String result = controller.order(
                List.of(8),
                List.of(1),
                "takeaway",
                "",
                null,
                "It da",
                customerSession(),
                new RedirectAttributesModelMap());

        assertEquals("redirect:/customer/payment/31", result);
        verify(notificationService, never()).notifyNewOrder(any());
    }

    @Test
    void choosingPaymentMethodForDraftNotifiesStaffOnce() throws Exception {
        OrderDAO orderDAO = mock(OrderDAO.class);
        OrderNotificationService notificationService = mock(OrderNotificationService.class);
        CustomerController controller = createController(mock(ProductDAO.class), orderDAO, notificationService);
        Order draft = pendingOrder(null);
        when(orderDAO.getOrderById(31)).thenReturn(draft);

        String result = controller.choosePaymentMethod(
                31,
                "momo",
                "takeaway",
                null,
                null,
                customerSession(),
                new RedirectAttributesModelMap());

        assertEquals("redirect:/customer/payment/31", result);
        ArgumentCaptor<OrderNotification> notification = ArgumentCaptor.forClass(OrderNotification.class);
        verify(notificationService).notifyNewOrder(notification.capture());
        assertEquals(31, notification.getValue().orderId());
        assertEquals("40,000 VND", notification.getValue().totalFormatted());
    }

    @Test
    void revisitingSubmittedPaymentDoesNotDuplicateNotification() throws Exception {
        OrderDAO orderDAO = mock(OrderDAO.class);
        OrderNotificationService notificationService = mock(OrderNotificationService.class);
        CustomerController controller = createController(mock(ProductDAO.class), orderDAO, notificationService);
        when(orderDAO.getOrderById(31)).thenReturn(pendingOrder("momo"));

        controller.choosePaymentMethod(
                31,
                "momo",
                "takeaway",
                null,
                null,
                customerSession(),
                new RedirectAttributesModelMap());

        verify(notificationService, never()).notifyNewOrder(any());
    }

    private CustomerController createController(ProductDAO productDAO,
                                                OrderDAO orderDAO,
                                                OrderNotificationService notificationService) {
        return new CustomerController(
                productDAO,
                orderDAO,
                mock(FeedbackDAO.class),
                notificationService,
                mock(QrCodeService.class),
                mock(VietQrPayloadService.class));
    }

    private MockHttpSession customerSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("role", "customer");
        session.setAttribute("customerId", 6);
        session.setAttribute("fullName", "Nguyen Trong Manh");
        return session;
    }

    private Order pendingOrder(String requestedPaymentMethod) {
        Order order = new Order();
        order.setOrderId(31);
        order.setCustomerId(6);
        order.setTotal(40000);
        order.setCustomerNote("It da");
        order.setRequestedPaymentMethod(requestedPaymentMethod);
        return order;
    }
}
