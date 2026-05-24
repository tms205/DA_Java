package com.huit.da_java.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomerPasswordServiceTest {
    private final CustomerPasswordService passwordService = new CustomerPasswordService();

    @Test
    void encodesAndVerifiesNewCustomerPassword() {
        String password = "coffee123";
        String encoded = passwordService.encode(password);

        assertNotEquals(password, encoded);
        assertTrue(encoded.startsWith("$2"));
        assertTrue(passwordService.matches(password, encoded));
        assertFalse(passwordService.matches("wrong-password", encoded));
        assertFalse(passwordService.needsUpgrade(encoded));
    }

    @Test
    void acceptsLegacyPlainTextPasswordOnlyForUpgrade() {
        assertTrue(passwordService.matches("123456", "123456"));
        assertFalse(passwordService.matches("654321", "123456"));
        assertTrue(passwordService.needsUpgrade("123456"));
    }
}
