package com.huit.da_java.controller;

import com.huit.da_java.dao.CustomerDAO;
import com.huit.da_java.dao.UserDAO;
import com.huit.da_java.model.Customer;
import com.huit.da_java.model.User;
import com.huit.da_java.service.OtpEmailService;
import com.huit.da_java.service.OtpService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class LoginController {
    private static final String REGISTRATION_OTP = "customer-registration";
    private static final String CUSTOMER_PASSWORD_OTP = "customer-password-reset";
    private static final String INTERNAL_PASSWORD_OTP = "internal-password-reset";
    private static final String PENDING_CUSTOMER = "pendingCustomerRegistration";
    private static final String CUSTOMER_RESET_PHONE = "customerResetPhone";
    private static final String CUSTOMER_RESET_EMAIL = "customerResetEmail";
    private static final String CUSTOMER_RESET_VERIFIED = "customerResetVerified";
    private static final String INTERNAL_RESET_USERNAME = "internalResetUsername";
    private static final String INTERNAL_RESET_EMAIL = "internalResetEmail";
    private static final String INTERNAL_RESET_VERIFIED = "internalResetVerified";

    private final UserDAO userDAO;
    private final CustomerDAO customerDAO;
    private final OtpService otpService;
    private final OtpEmailService otpEmailService;

    public LoginController(UserDAO userDAO,
                           CustomerDAO customerDAO,
                           OtpService otpService,
                           OtpEmailService otpEmailService) {
        this.userDAO = userDAO;
        this.customerDAO = customerDAO;
        this.otpService = otpService;
        this.otpEmailService = otpEmailService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        String role = (String) session.getAttribute("role");
        if ("admin".equals(role)) {
            return "redirect:/admin/dashboard";
        }
        if ("staff".equals(role)) {
            return "redirect:/staff/pos";
        }
        if ("customer".equals(role)) {
            return "redirect:/customer/menu";
        }
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model) {
        return "login";
    }

    @GetMapping("/login/internal")
    public String internalLogin() {
        return "login-internal";
    }

    @GetMapping("/login/customer")
    public String customerLoginPage(Model model) {
        model.addAttribute("customer", new Customer());
        return "login-customer";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(@RequestParam(defaultValue = "customer") String accountType) {
        return "internal".equals(accountType)
                ? "redirect:/forgot-password/internal"
                : "redirect:/forgot-password/customer";
    }

    @GetMapping("/forgot-password/customer")
    public String customerForgotPasswordPage() {
        return "forgot-password-customer";
    }

    @GetMapping("/forgot-password/internal")
    public String internalForgotPasswordPage() {
        return "forgot-password-internal";
    }

    @PostMapping("/login/staff")
    public String staffLogin(@RequestParam String username,
                             @RequestParam String password,
                             HttpSession session,
                             RedirectAttributes redirectAttributes) {
        try {
            User user = userDAO.login(username.trim(), password.trim());
            if (user == null) {
                redirectAttributes.addFlashAttribute("error", "Sai tên đăng nhập hoặc mật khẩu.");
                return "redirect:/login/internal";
            }
            session.setAttribute("userId", user.getUserId());
            session.setAttribute("username", user.getUsername());
            session.setAttribute("fullName", user.getFullName());
            session.setAttribute("role", user.getRole());
            return "admin".equals(user.getRole()) ? "redirect:/admin/dashboard" : "redirect:/staff/pos";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể đăng nhập: " + ex.getMessage());
            return "redirect:/login/internal";
        }
    }

    @PostMapping("/login/customer")
    public String customerLogin(@RequestParam String phone,
                                @RequestParam String password,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        try {
            Customer customer = customerDAO.login(phone.trim(), password.trim());
            if (customer == null) {
                redirectAttributes.addFlashAttribute("error", "Số điện thoại hoặc mật khẩu không đúng.");
                return "redirect:/login/customer";
            }
            session.setAttribute("customerId", customer.getCustomerId());
            session.setAttribute("username", customer.getPhone());
            session.setAttribute("fullName", customer.getFullName());
            session.setAttribute("customerAddress", customer.getAddress());
            session.setAttribute("role", "customer");
            return "redirect:/customer/menu";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể đăng nhập: " + ex.getMessage());
            return "redirect:/login/customer";
        }
    }

    @PostMapping("/register")
    public String register(@ModelAttribute Customer customer,
                           HttpSession session,
                           RedirectAttributes redirectAttributes) {
        if (customer.getFullName() == null || customer.getFullName().isBlank()
                || customer.getPhone() == null || !customer.getPhone().matches("\\d{10,11}")
                || customer.getPassword() == null || customer.getPassword().trim().length() < 4
                || !isValidEmail(customer.getEmail())) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng nhập họ tên, số điện thoại, email hợp lệ và mật khẩu từ 4 ký tự.");
            return "redirect:/login/customer";
        }
        try {
            customer.setPhone(customer.getPhone().trim());
            customer.setEmail(customer.getEmail().trim().toLowerCase());
            if (customerDAO.existsByPhone(customer.getPhone())) {
                throw new IllegalArgumentException("Số điện thoại đã được đăng ký.");
            }
            if (customerDAO.existsByEmail(customer.getEmail())) {
                throw new IllegalArgumentException("Email đã được sử dụng cho tài khoản khác.");
            }
            session.setAttribute(PENDING_CUSTOMER, customer);
            sendOtp(REGISTRATION_OTP, customer.getPhone(), customer.getEmail(), "dang ky tai khoan");
            redirectAttributes.addFlashAttribute("success", "Mã OTP đã được gửi tới email. Nhập mã để hoàn tất đăng ký.");
            return "redirect:/register/verify";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Đăng ký thất bại: " + ex.getMessage());
            return "redirect:/login/customer";
        }
    }

    @GetMapping("/register/verify")
    public String registrationVerificationPage(HttpSession session) {
        return session.getAttribute(PENDING_CUSTOMER) instanceof Customer
                ? "register-verify-otp"
                : "redirect:/login/customer";
    }

    @PostMapping("/register/verify")
    public String verifyRegistration(@RequestParam String otp,
                                     HttpSession session,
                                     RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute(PENDING_CUSTOMER);
        if (customer == null) {
            return "redirect:/login/customer";
        }
        try {
            OtpService.VerificationResult result = otpService.verify(REGISTRATION_OTP, customer.getPhone(), otp);
            if (result != OtpService.VerificationResult.VERIFIED) {
                redirectAttributes.addFlashAttribute("error", otpError(result));
                return "redirect:/register/verify";
            }
            int customerId = customerDAO.register(customer);
            session.removeAttribute(PENDING_CUSTOMER);
            if (customerId <= 0) {
                throw new IllegalStateException("Không thể tạo tài khoản khách hàng.");
            }
            redirectAttributes.addFlashAttribute("success", "Email đã được xác thực và tài khoản đã được tạo. Bạn có thể đăng nhập.");
            return "redirect:/login/customer";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể hoàn tất đăng ký: " + ex.getMessage());
            return "redirect:/register/verify";
        }
    }

    @PostMapping("/register/resend")
    public String resendRegistrationOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        Customer customer = (Customer) session.getAttribute(PENDING_CUSTOMER);
        if (customer == null) {
            return "redirect:/login/customer";
        }
        try {
            sendOtp(REGISTRATION_OTP, customer.getPhone(), customer.getEmail(), "dang ky tai khoan");
            redirectAttributes.addFlashAttribute("success", "Đã gửi lại mã OTP mới.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể gửi lại OTP: " + ex.getMessage());
        }
        return "redirect:/register/verify";
    }

    @PostMapping("/forgot-password/customer")
    public String requestCustomerPasswordOtp(@RequestParam String phone,
                                             @RequestParam String email,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        try {
            if (phone == null || !phone.trim().matches("\\d{10,11}") || !isValidEmail(email)) {
                throw new IllegalArgumentException("Vui lòng nhập số điện thoại và email hợp lệ.");
            }
            String normalizedPhone = phone.trim();
            String normalizedEmail = email.trim().toLowerCase();
            Customer customer = customerDAO.findActiveByPhoneAndEmail(normalizedPhone, normalizedEmail);
            if (customer == null) {
                clearCustomerReset(session);
                throw new IllegalArgumentException("Số điện thoại và email không trùng với tài khoản đã đăng ký.");
            }
            session.setAttribute(CUSTOMER_RESET_PHONE, normalizedPhone);
            session.setAttribute(CUSTOMER_RESET_EMAIL, normalizedEmail);
            session.removeAttribute(CUSTOMER_RESET_VERIFIED);
            sendOtp(CUSTOMER_PASSWORD_OTP, normalizedPhone, customer.getEmail(), "dat lai mat khau");
            redirectAttributes.addFlashAttribute("success", "Mã OTP đã được gửi tới email đã đăng ký của tài khoản.");
            return "redirect:/forgot-password/customer/verify";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể gửi mã OTP: " + ex.getMessage());
            return "redirect:/forgot-password/customer";
        }
    }

    @GetMapping("/forgot-password/customer/verify")
    public String customerPasswordVerificationPage(HttpSession session) {
        return session.getAttribute(CUSTOMER_RESET_PHONE) != null
                ? "forgot-password-verify-otp"
                : "redirect:/forgot-password/customer";
    }

    @PostMapping("/forgot-password/customer/verify")
    public String verifyCustomerPasswordOtp(@RequestParam String otp,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        String phone = (String) session.getAttribute(CUSTOMER_RESET_PHONE);
        if (phone == null) {
            return "redirect:/forgot-password/customer";
        }
        OtpService.VerificationResult result = otpService.verify(CUSTOMER_PASSWORD_OTP, phone, otp);
        if (result != OtpService.VerificationResult.VERIFIED) {
            redirectAttributes.addFlashAttribute("error", otpError(result));
            return "redirect:/forgot-password/customer/verify";
        }
        session.setAttribute(CUSTOMER_RESET_VERIFIED, true);
        return "redirect:/forgot-password/customer/reset";
    }

    @GetMapping("/forgot-password/customer/reset")
    public String customerPasswordResetPage(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(CUSTOMER_RESET_VERIFIED))
                ? "forgot-password-customer-reset"
                : "redirect:/forgot-password/customer";
    }

    @PostMapping("/forgot-password/customer/reset")
    public String resetCustomerPassword(@RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute(CUSTOMER_RESET_VERIFIED))) {
            return "redirect:/forgot-password/customer";
        }
        if (!validNewPassword(newPassword, confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới cần từ 4 ký tự và hai lần nhập phải trùng nhau.");
            return "redirect:/forgot-password/customer/reset";
        }
        try {
            boolean updated = customerDAO.resetPassword(
                    (String) session.getAttribute(CUSTOMER_RESET_PHONE),
                    (String) session.getAttribute(CUSTOMER_RESET_EMAIL),
                    newPassword.trim());
            clearCustomerReset(session);
            if (!updated) {
                throw new IllegalStateException("Không thể cập nhật tài khoản.");
            }
            redirectAttributes.addFlashAttribute("success", "Đã đặt lại mật khẩu. Bạn có thể đăng nhập bằng mật khẩu mới.");
            return "redirect:/login/customer";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể đặt lại mật khẩu: " + ex.getMessage());
            return "redirect:/forgot-password/customer/reset";
        }
    }

    @PostMapping("/forgot-password/internal")
    public String requestInternalPasswordOtp(@RequestParam String username,
                                             @RequestParam String email,
                                             HttpSession session,
                                             RedirectAttributes redirectAttributes) {
        try {
            if (username == null || username.isBlank() || !isValidEmail(email)) {
                throw new IllegalArgumentException("Vui lòng nhập tài khoản và email hợp lệ.");
            }
            String normalizedUsername = username.trim();
            String normalizedEmail = email.trim().toLowerCase();
            User user = userDAO.findActiveByUsernameAndEmail(normalizedUsername, normalizedEmail);
            if (user == null) {
                clearInternalReset(session);
                throw new IllegalArgumentException("Tài khoản và email không trùng với thông tin đã đăng ký.");
            }
            session.setAttribute(INTERNAL_RESET_USERNAME, normalizedUsername);
            session.setAttribute(INTERNAL_RESET_EMAIL, normalizedEmail);
            session.removeAttribute(INTERNAL_RESET_VERIFIED);
            sendOtp(INTERNAL_PASSWORD_OTP, normalizedUsername, user.getEmail(), "dat lai mat khau noi bo");
            redirectAttributes.addFlashAttribute("success", "Mã OTP đã được gửi tới email đã đăng ký của tài khoản.");
            return "redirect:/forgot-password/internal/verify";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể gửi mã OTP: " + ex.getMessage());
            return "redirect:/forgot-password/internal";
        }
    }

    @GetMapping("/forgot-password/internal/verify")
    public String internalPasswordVerificationPage(HttpSession session) {
        return session.getAttribute(INTERNAL_RESET_USERNAME) != null
                ? "forgot-password-internal-verify-otp"
                : "redirect:/forgot-password/internal";
    }

    @PostMapping("/forgot-password/internal/verify")
    public String verifyInternalPasswordOtp(@RequestParam String otp,
                                            HttpSession session,
                                            RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute(INTERNAL_RESET_USERNAME);
        if (username == null) {
            return "redirect:/forgot-password/internal";
        }
        OtpService.VerificationResult result = otpService.verify(INTERNAL_PASSWORD_OTP, username, otp);
        if (result != OtpService.VerificationResult.VERIFIED) {
            redirectAttributes.addFlashAttribute("error", otpError(result));
            return "redirect:/forgot-password/internal/verify";
        }
        session.setAttribute(INTERNAL_RESET_VERIFIED, true);
        return "redirect:/forgot-password/internal/reset";
    }

    @GetMapping("/forgot-password/internal/reset")
    public String internalPasswordResetPage(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(INTERNAL_RESET_VERIFIED))
                ? "forgot-password-internal-reset"
                : "redirect:/forgot-password/internal";
    }

    @PostMapping("/forgot-password/internal/reset")
    public String resetInternalPassword(@RequestParam String newPassword,
                                        @RequestParam String confirmPassword,
                                        HttpSession session,
                                        RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute(INTERNAL_RESET_VERIFIED))) {
            return "redirect:/forgot-password/internal";
        }
        if (!validNewPassword(newPassword, confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu mới cần từ 4 ký tự và hai lần nhập phải trùng nhau.");
            return "redirect:/forgot-password/internal/reset";
        }
        try {
            boolean updated = userDAO.resetPassword(
                    (String) session.getAttribute(INTERNAL_RESET_USERNAME),
                    (String) session.getAttribute(INTERNAL_RESET_EMAIL),
                    newPassword.trim());
            clearInternalReset(session);
            if (!updated) {
                throw new IllegalStateException("Không thể cập nhật tài khoản.");
            }
            redirectAttributes.addFlashAttribute("success", "Đã đặt lại mật khẩu. Bạn có thể đăng nhập bằng mật khẩu mới.");
            return "redirect:/login/internal";
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("error", "Không thể đặt lại mật khẩu: " + ex.getMessage());
            return "redirect:/forgot-password/internal/reset";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/login";
    }

    private void sendOtp(String purpose, String identity, String recipient, String action) {
        String code = otpService.issue(purpose, identity);
        deliverOtp(purpose, identity, recipient, action, code);
    }

    private void deliverOtp(String purpose, String identity, String recipient, String action, String code) {
        try {
            otpEmailService.sendOtp(recipient, action, code);
        } catch (RuntimeException ex) {
            otpService.invalidate(purpose, identity);
            String reason = ex.getMessage() == null || ex.getMessage().isBlank()
                    ? "Vui lòng kiểm tra Gmail SMTP và Gmail App Password."
                    : ex.getMessage();
            throw new IllegalStateException("Không gửi được email. " + reason, ex);
        }
    }

    private String otpError(OtpService.VerificationResult result) {
        return switch (result) {
            case INVALID -> "Mã OTP không đúng. Vui lòng nhập lại.";
            case EXPIRED -> "Mã OTP đã hết hạn. Vui lòng thực hiện gửi mã mới.";
            case TOO_MANY_ATTEMPTS -> "Bạn đã nhập sai quá nhiều lần. Vui lòng thực hiện gửi mã mới.";
            case VERIFIED -> "";
        };
    }

    private boolean isValidEmail(String email) {
        return email != null && email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean validNewPassword(String password, String confirmation) {
        return password != null && password.trim().length() >= 4 && password.equals(confirmation);
    }

    private void clearCustomerReset(HttpSession session) {
        session.removeAttribute(CUSTOMER_RESET_PHONE);
        session.removeAttribute(CUSTOMER_RESET_EMAIL);
        session.removeAttribute(CUSTOMER_RESET_VERIFIED);
    }

    private void clearInternalReset(HttpSession session) {
        session.removeAttribute(INTERNAL_RESET_USERNAME);
        session.removeAttribute(INTERNAL_RESET_EMAIL);
        session.removeAttribute(INTERNAL_RESET_VERIFIED);
    }
}
