package com.rvmc.ordertaker.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    @Test
    void acceptsTwelveCharacterPassword() {
        assertDoesNotThrow(() -> AuthService.validatePassword("abcdefghijkl"));
    }

    @Test
    void rejectsPasswordShorterThanTwelveCharacters() {
        assertThrows(AuthService.InvalidPasswordException.class,
                () -> AuthService.validatePassword("abcdefghijk"));
    }

    @Test
    void rejectsPasswordsOverBcryptUtf8ByteLimit() {
        assertThrows(AuthService.InvalidPasswordException.class,
                () -> AuthService.validatePassword("😀".repeat(19)));
    }

    @Test
    void acceptsExactlySeventyTwoUtf8Bytes() {
        assertDoesNotThrow(() -> AuthService.validatePassword("😀".repeat(18)));
    }

    @Test
    void countsUnicodeCodePointsForMinimumLength() {
        assertDoesNotThrow(() -> AuthService.validatePassword("😀".repeat(12)));
    }
}
