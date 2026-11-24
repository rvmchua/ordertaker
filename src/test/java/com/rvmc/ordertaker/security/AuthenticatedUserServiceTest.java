package com.rvmc.ordertaker.security;

import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.exception.ForbiddenException;
import com.rvmc.ordertaker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticatedUserServiceTest {
    private UserRepository userRepository;
    private AuthenticatedUserService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        service = new AuthenticatedUserService(userRepository);
    }

    @Test
    void getCustomer_shouldReturnCustomerFromAuthenticatedEmail() {
        Customer customer = Customer.builder().customerId(5L).build();
        User user = User.builder().email("customer@example.com").customer(customer).build();
        customer.setUser(user);
        when(userRepository.findByEmail("customer@example.com")).thenReturn(Optional.of(user));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "customer@example.com", null, List.of()
        );

        assertEquals(5L, service.getCustomer(authentication).getCustomerId());
    }

    @Test
    void getRestaurant_shouldReturnRestaurantFromAuthenticatedEmail() {
        Restaurant restaurant = Restaurant.builder().restaurantId(9L).build();
        User user = User.builder().email("owner@example.com").restaurant(restaurant).build();
        restaurant.setOwner(user);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(user));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "owner@example.com", null, List.of()
        );

        assertEquals(9L, service.getRestaurant(authentication).getRestaurantId());
    }

    @Test
    void getCustomer_shouldRejectAuthenticatedNonCustomer() {
        Restaurant restaurant = Restaurant.builder().restaurantId(9L).build();
        User owner = User.builder().email("owner@example.com").restaurant(restaurant).build();
        restaurant.setOwner(owner);
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                "owner@example.com", null, List.of()
        );

        assertThrows(ForbiddenException.class, () -> service.getCustomer(authentication));
    }
}
