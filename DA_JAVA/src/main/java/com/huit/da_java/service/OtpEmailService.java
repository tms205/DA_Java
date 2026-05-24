package com.huit.da_java.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpEmailService {
    private final JavaMailSender mailSender;
    private final String from;
    private final String smtpPassword;

    public OtpEmailService(JavaMailSender mailSender,
                           @Value("${app.mail.from:}") String from,
                           @Value("${spring.mail.password:}") String smtpPassword) {
        this.mailSender = mailSender;
        this.from = from;
        this.smtpPassword = smtpPassword;
    }

    public void sendOtp(String recipient, String subjectAction, String code) {
        if (smtpPassword == null || smtpPassword.isBlank()) {
            throw new IllegalStateException("Chưa cấu hình MAIL_PASSWORD bằng Gmail App Password 16 ký tự.");
        }
        SimpleMailMessage message = new SimpleMailMessage();
        if (from != null && !from.isBlank()) {
            message.setFrom(from);
        }
        message.setTo(recipient);
        message.setSubject("Cafe Management - Ma xac thuc " + subjectAction);
        message.setText("""
                Xin chao,

                Ma xac thuc cua ban la: %s

                Ma co hieu luc trong 5 phut va chi dung mot lan.
                Neu ban khong yeu cau thao tac nay, vui long bo qua email.
                """.formatted(code));
        mailSender.send(message);
    }
}
