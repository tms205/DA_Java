package com.huit.da_java.controller;

import com.huit.da_java.model.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LoginControllerPageTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void customerLoginKeepsFlipCardNavigation() throws Exception {
        mockMvc.perform(get("/login/customer"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("flip-card-inner")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("customer-register-mode")));
    }

    @Test
    void registrationOtpPageRendersForPendingRegistration() throws Exception {
        mockMvc.perform(get("/register/verify")
                        .sessionAttr("pendingCustomerRegistration", new Customer()))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Hoàn tất đăng ký")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("otp-code-input")));
    }

    @Test
    void customerOtpVerificationPageRendersForRequestedReset() throws Exception {
        mockMvc.perform(get("/forgot-password/customer/verify")
                        .sessionAttr("customerResetPhone", "0901234567"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác minh OTP")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("data-otp-digit")));
    }

    @Test
    void customerResetPageRendersOnlyAfterOtpVerification() throws Exception {
        mockMvc.perform(get("/forgot-password/customer/reset")
                        .sessionAttr("customerResetVerified", true))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo mật khẩu mới")));
    }

    @Test
    void internalOtpVerificationPageRendersForRequestedReset() throws Exception {
        mockMvc.perform(get("/forgot-password/internal/verify")
                        .sessionAttr("internalResetUsername", "staff1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Xác minh OTP")));
    }

    @Test
    void internalResetPageRendersOnlyAfterOtpVerification() throws Exception {
        mockMvc.perform(get("/forgot-password/internal/reset")
                        .sessionAttr("internalResetVerified", true))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Tạo mật khẩu mới")));
    }
}
