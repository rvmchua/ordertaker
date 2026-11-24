package com.rvmc.ordertaker.security;

import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class JwtServiceTest {
    private static final String SECRET =
            "test-secret-key-for-jwt-signing-must-be-long-enough";

    private final JwtService jwtService = new JwtService(SECRET, 60_000);
    private final SecretKey signingKey =
            Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));

    @Test
    void generateToken_shouldContainSignedSubjectAndRoleClaims() {
        User user = User.builder()
                .userId(7L)
                .email("customer@example.com")
                .role(Role.CUSTOMER)
                .build();

        String token = jwtService.generateToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("customer@example.com", claims.getSubject());
        assertEquals("CUSTOMER", claims.get("role", String.class));
        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());
        assertFalse(token.isBlank());
    }

    @Test
    void generateToken_shouldEncodeRestaurantOwnerRole() {
        User owner = User.builder()
                .email("owner@example.com")
                .role(Role.RESTAURANT_OWNER)
                .build();

        String token = jwtService.generateToken(owner);

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals("owner@example.com", claims.getSubject());
        assertEquals("RESTAURANT_OWNER", claims.get("role", String.class));
    }
}
