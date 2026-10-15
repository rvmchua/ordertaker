package com.rvmc.ordertaker.security;

import com.rvmc.ordertaker.entity.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtException;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {
    public static final Duration TOKEN_LIFETIME = Duration.ofMinutes(15);

    private final SecretKey signingKey;
    private final Clock clock;

    public JwtService(SecretKey signingKey, Clock clock) {
        this.signingKey = signingKey;
        this.clock = clock;
    }

    public String issue(long userId, Role role) {
        Instant issuedAt = clock.instant();
        return Jwts.builder()
                .subject(Long.toString(userId))
                .claim("role", role.name())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(issuedAt.plus(TOKEN_LIFETIME)))
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    public AuthenticatedUser parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long userId;
        Role role;
        try {
            userId = Long.parseLong(claims.getSubject());
            role = Role.valueOf(claims.get("role", String.class));
        } catch (RuntimeException exception) {
            throw new JwtException("Invalid token claims", exception);
        }
        return new AuthenticatedUser(userId, role);
    }

    public record AuthenticatedUser(long userId, Role role) {
    }
}
