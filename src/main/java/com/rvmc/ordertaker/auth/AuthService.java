package com.rvmc.ordertaker.auth;

import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.entity.enums.Role;
import com.rvmc.ordertaker.repository.UserRepository;
import com.rvmc.ordertaker.security.JwtService;
import org.springframework.dao.DataIntegrityViolationException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class AuthService {
    private static final int MIN_PASSWORD_CODE_POINTS = 12;
    private static final int MAX_PASSWORD_BYTES = 72;
    private static final String DUMMY_PASSWORD = "ordertaker-invalid-login";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final String dummyPasswordHash;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.dummyPasswordHash = passwordEncoder.encode(DUMMY_PASSWORD);
    }

    @Transactional
    public void registerCustomer(CustomerSignupRequest request) {
        validatePassword(request.password());
        User user = newUser(request.username(), request.email(), request.password(), Role.CUSTOMER);
        Customer customer = Customer.builder()
                .customerName(request.customerName().trim())
                .address1(request.address1().trim())
                .address2(trimToNull(request.address2()))
                .isActive(true)
                .build();
        user.setCustomer(customer);
        saveNewUser(user);
    }

    @Transactional
    public void registerRestaurant(RestaurantSignupRequest request) {
        validatePassword(request.password());
        User user = newUser(request.username(), request.email(), request.password(), Role.RESTAURANT);
        Restaurant restaurant = Restaurant.builder()
                .restaurantName(request.restaurantName().trim())
                .address1(request.address1().trim())
                .address2(trimToNull(request.address2()))
                .isActive(true)
                .toDelete(false)
                .build();
        user.setRestaurant(restaurant);
        saveNewUser(user);
    }

    @Transactional(readOnly = true)
    public TokenResponse login(String email, String password, Role expectedRole) {
        String normalizedEmail = normalizeEmail(email);
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);
        boolean withinBcryptLimit = password.getBytes(StandardCharsets.UTF_8).length <= MAX_PASSWORD_BYTES;
        String candidate = withinBcryptLimit ? password : DUMMY_PASSWORD;
        String passwordHash = user == null || !withinBcryptLimit
                ? dummyPasswordHash
                : user.getPasswordHash();
        boolean passwordMatches = passwordEncoder.matches(candidate, passwordHash);
        if (!withinBcryptLimit || user == null || user.getRole() != expectedRole || !isActive(user)
                || !passwordMatches) {
            throw new InvalidCredentialsException();
        }
        return TokenResponse.bearer(jwtService.issue(user.getUserId(), user.getRole()));
    }

    private User newUser(String username, String email, String password, Role role) {
        return User.builder()
                .username(username.trim())
                .email(normalizeEmail(email))
                .passwordHash(passwordEncoder.encode(password))
                .role(role)
                .build();
    }

    private void saveNewUser(User user) {
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new AuthConflictException("An account with this email already exists");
        }
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueConstraint(exception)) {
                throw new AuthConflictException("An account with this email already exists");
            }
            throw exception;
        }
    }

    private static boolean isEmailUniqueConstraint(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return "uk_users_email".equalsIgnoreCase(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }

    private static boolean isActive(User user) {
        return switch (user.getRole()) {
            case CUSTOMER -> user.getCustomer() != null && Boolean.TRUE.equals(user.getCustomer().getIsActive());
            case RESTAURANT -> user.getRestaurant() != null
                    && Boolean.TRUE.equals(user.getRestaurant().getIsActive());
        };
    }

    static void validatePassword(String password) {
        int codePoints = password.codePointCount(0, password.length());
        int byteLength = password.getBytes(StandardCharsets.UTF_8).length;
        if (codePoints < MIN_PASSWORD_CODE_POINTS) {
            throw new InvalidPasswordException("Password must contain at least 12 characters");
        }
        if (byteLength > MAX_PASSWORD_BYTES) {
            throw new InvalidPasswordException("Password must not exceed 72 UTF-8 bytes");
        }
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid email or password");
        }
    }

    public static class InvalidPasswordException extends RuntimeException {
        public InvalidPasswordException(String message) {
            super(message);
        }
    }
}
