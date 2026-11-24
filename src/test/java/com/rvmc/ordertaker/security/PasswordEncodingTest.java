package com.rvmc.ordertaker.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordEncodingTest {
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void shouldHashPasswordAndVerifyItWithBCrypt() {
        String rawPassword = "secure-password";

        String hash = passwordEncoder.encode(rawPassword);

        assertNotEquals(rawPassword, hash);
        assertTrue(passwordEncoder.matches(rawPassword, hash));
    }
}
