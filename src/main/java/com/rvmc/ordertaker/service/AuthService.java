package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.AuthResponse;
import com.rvmc.ordertaker.dto.CustomerRegistrationRequest;
import com.rvmc.ordertaker.dto.LoginRequest;
import com.rvmc.ordertaker.dto.RestaurantOwnerRegistrationRequest;
import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.entity.enums.Role;
import com.rvmc.ordertaker.exception.DuplicateEmailException;
import com.rvmc.ordertaker.exception.InvalidCredentialsException;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import com.rvmc.ordertaker.repository.UserRepository;
import com.rvmc.ordertaker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    /**
     * Registers a customer, hashes their password, and creates a signed access token.
     *
     * @param request customer registration details
     * @return authentication response
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public AuthResponse registerCustomer(CustomerRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = User.builder()
                .username(request.username())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.CUSTOMER)
                .build();

        Customer customer = Customer.builder()
                .customerName(request.customerName())
                .address1(request.address1())
                .address2(request.address2())
                .isActive(true)
                .build();
        user.setCustomer(customer);

        try {
            user = userRepository.save(user);
            customerRepository.save(customer);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(email);
        }
        return toAuthResponse(user);
    }

    /**
     * Registers a restaurant owner and their restaurant profile.
     *
     * @param request restaurant-owner registration details
     * @return authentication response
     * @throws DuplicateEmailException if the email is already registered
     */
    @Transactional
    public AuthResponse registerRestaurantOwner(RestaurantOwnerRegistrationRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException(email);
        }

        User user = User.builder()
                .username(request.username())
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.RESTAURANT)
                .build();

        Restaurant restaurant = Restaurant.builder()
                .restaurantName(request.restaurantName())
                .address1(request.address1())
                .address2(request.address2())
                .isActive(true)
                .toDelete(false)
                .build();
        user.setRestaurant(restaurant);

        try {
            user = userRepository.save(user);
            restaurantRepository.save(restaurant);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEmailException(email);
        }
        return toAuthResponse(user);
    }

    /**
     * Authenticates a user and creates a signed access token.
     *
     * @param request login credentials
     * @return authentication response
     * @throws InvalidCredentialsException if the email or password is invalid
     */
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(InvalidCredentialsException::new);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                user.getUserId(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
