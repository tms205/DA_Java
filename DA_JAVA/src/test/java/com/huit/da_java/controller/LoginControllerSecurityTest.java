package com.huit.da_java.controller;

import com.huit.da_java.dao.CustomerDAO;
import com.huit.da_java.dao.UserDAO;
import com.huit.da_java.model.Customer;
import com.huit.da_java.service.OtpEmailService;
import com.huit.da_java.service.OtpService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LoginControllerSecurityTest {

    @Test
    void doesNotCreateOrSendCustomerOtpWhenPhoneAndEmailDoNotMatch() throws Exception {
        CustomerDAO customerDAO = mock(CustomerDAO.class);
        OtpService otpService = mock(OtpService.class);
        OtpEmailService emailService = mock(OtpEmailService.class);
        LoginController controller = new LoginController(mock(UserDAO.class), customerDAO, otpService, emailService);
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();

        when(customerDAO.findActiveByPhoneAndEmail("0901234567", "wrong@example.com")).thenReturn(null);

        String result = controller.requestCustomerPasswordOtp(
                "0901234567",
                "wrong@example.com",
                session,
                redirectAttributes);

        assertEquals("redirect:/forgot-password/customer", result);
        assertNull(session.getAttribute("customerResetPhone"));
        verify(otpService, never()).issue("customer-password-reset", "0901234567");
        verify(emailService, never()).sendOtp(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void registrationRejectsAnEmailAlreadyRegisteredToAnotherCustomer() throws Exception {
        CustomerDAO customerDAO = mock(CustomerDAO.class);
        OtpService otpService = mock(OtpService.class);
        OtpEmailService emailService = mock(OtpEmailService.class);
        LoginController controller = new LoginController(mock(UserDAO.class), customerDAO, otpService, emailService);
        Customer customer = new Customer();
        customer.setFullName("Nguyen Van A");
        customer.setPhone("0901234567");
        customer.setEmail("registered@example.com");
        customer.setPassword("secure123");

        when(customerDAO.existsByPhone("0901234567")).thenReturn(false);
        when(customerDAO.existsByEmail("registered@example.com")).thenReturn(true);

        String result = controller.register(customer, new MockHttpSession(), new RedirectAttributesModelMap());

        assertEquals("redirect:/login/customer", result);
        verify(otpService, never()).issue("customer-registration", "0901234567");
        verify(emailService, never()).sendOtp(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doesNotCreateOrSendInternalOtpWhenAccountAndEmailDoNotMatch() throws Exception {
        UserDAO userDAO = mock(UserDAO.class);
        OtpService otpService = mock(OtpService.class);
        OtpEmailService emailService = mock(OtpEmailService.class);
        LoginController controller = new LoginController(userDAO, mock(CustomerDAO.class), otpService, emailService);

        when(userDAO.findActiveByUsernameAndEmail("staff1", "wrong@example.com")).thenReturn(null);

        String result = controller.requestInternalPasswordOtp(
                "staff1",
                "wrong@example.com",
                new MockHttpSession(),
                new RedirectAttributesModelMap());

        assertEquals("redirect:/forgot-password/internal", result);
        verify(otpService, never()).issue("internal-password-reset", "staff1");
        verify(emailService, never()).sendOtp(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString());
    }
}
