package com.rvmc.ordertaker.auth;

import com.rvmc.ordertaker.entity.enums.Role;
import com.rvmc.ordertaker.security.JwtService;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {
    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    private JwtService service(Clock clock) {
        return new JwtService(
                new SecretKeySpec(new byte[32], "HmacSHA256"),
                clock
        );
    }

    @Test
    void issuedTokenContainsOnlyExpectedIdentityAndRole() {
        JwtService service = service(Clock.fixed(NOW, ZoneOffset.UTC));
        String token = service.issue(42L, Role.CUSTOMER);

        JwtService.AuthenticatedUser user = service.parse(token);

        assertEquals(42L, user.userId());
        assertEquals(Role.CUSTOMER, user.role());
    }

    @Test
    void rejectsTokenAfterFifteenMinuteExpiry() {
        JwtService issuer = service(Clock.fixed(NOW, ZoneOffset.UTC));
        String token = issuer.issue(42L, Role.RESTAURANT);
        JwtService expiredVerifier = service(Clock.fixed(NOW.plusSeconds(901), ZoneOffset.UTC));

        assertThrows(io.jsonwebtoken.ExpiredJwtException.class, () -> expiredVerifier.parse(token));
    }
}
