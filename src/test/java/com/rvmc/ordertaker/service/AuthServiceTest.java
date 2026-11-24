package com.rvmc.ordertaker.service;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

class AuthServiceTest {
    private BCryptPasswordEncoder passwordEncoder;
    private UserRepository userRepository;
    private CustomerRepository customerRepository;
    private RestaurantRepository restaurantRepository;
    private JwtService jwtService;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        userRepository = mock(UserRepository.class);
        customerRepository = mock(CustomerRepository.class);
        restaurantRepository = mock(RestaurantRepository.class);
        jwtService = mock(JwtService.class);
        authService = new AuthService(
                userRepository,
                customerRepository,
                restaurantRepository,
                passwordEncoder,
                jwtService
        );
    }

    @Test
    void customerRegistration_shouldEncodePasswordAndCreateCustomer() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(7L);
            return user;
        });
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("customer-token");

        var response = authService.registerCustomer(
                new CustomerRegistrationRequest(
                        "customer",
                        "  Customer@Example.COM ",
                        "password123",
                        "Customer Name",
                        "Address 1",
                        "Address 2"
                )
        );

        assertEquals("customer-token", response.accessToken());
        assertEquals("customer@example.com", response.email());
        assertEquals(Role.CUSTOMER.name(), response.role());
        assertNotNull(response.userId());
        verify(userRepository).save(argThat(user ->
                "customer@example.com".equals(user.getEmail())
                        && Role.CUSTOMER == user.getRole()
                        && passwordEncoder.matches("password123", user.getPasswordHash())
        ));
        verify(customerRepository).save(argThat(customer ->
                "Customer Name".equals(customer.getCustomerName())
                        && "Address 1".equals(customer.getAddress1())
        ));
    }

    @Test
    void customerRegistration_shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("customer@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.registerCustomer(
                new CustomerRegistrationRequest(
                        "customer",
                        "customer@example.com",
                        "password123",
                        "Customer Name",
                        null,
                        null
                )
        ));
        verifyNoInteractions(customerRepository);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void restaurantOwnerRegistration_shouldUseOwnerRole() {
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setUserId(12L);
            return user;
        });
        when(jwtService.generateToken(any(User.class))).thenReturn("signed-token");

        var response = authService.registerRestaurantOwner(
                new RestaurantOwnerRegistrationRequest(
                        "owner",
                        "owner@example.com",
                        "password123",
                        "Pizza Place",
                        null,
                        null
                )
        );

        assertEquals(Role.RESTAURANT_OWNER.name(), response.role());
        assertEquals("signed-token", response.accessToken());
        verify(restaurantRepository).save(argThat(restaurant ->
                "Pizza Place".equals(restaurant.getRestaurantName())
                        && Boolean.TRUE.equals(restaurant.getIsActive())
                        && Boolean.FALSE.equals(restaurant.getToDelete())
        ));
    }

    @Test
    void restaurantOwnerRegistration_shouldRejectDuplicateEmail() {
        when(userRepository.existsByEmail("owner@example.com")).thenReturn(true);

        assertThrows(DuplicateEmailException.class, () -> authService.registerRestaurantOwner(
                new RestaurantOwnerRegistrationRequest(
                        "owner",
                        "owner@example.com",
                        "password123",
                        "Pizza Place",
                        null,
                        null
                )
        ));
        verifyNoInteractions(restaurantRepository);
    }

    @Test
    void login_shouldReturnJwtResponse_whenCredentialsAreValid() {
        User user = User.builder()
                .userId(1L)
                .email("customer@example.com")
                .passwordHash(passwordEncoder.encode("password123"))
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("login-token");

        var response = authService.login(
                new LoginRequest("  Customer@Example.COM ", "password123")
        );

        assertEquals("login-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals(1L, response.userId());
        assertEquals("customer@example.com", response.email());
        assertEquals("CUSTOMER", response.role());
    }

    @Test
    void login_shouldRejectUnknownEmail() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("unknown@example.com", "password123")));
        verifyNoInteractions(jwtService);
    }

    @Test
    void login_shouldRejectInvalidPassword() {
        User user = User.builder()
                .userId(1L)
                .email("customer@example.com")
                .passwordHash(new BCryptPasswordEncoder().encode("password123"))
                .role(Role.CUSTOMER)
                .build();
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));

        assertThrows(InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("customer@example.com", "wrong-password")));
    }
}
