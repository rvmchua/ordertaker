package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.AuthResponse;
import com.rvmc.ordertaker.dto.CustomerRegistrationRequest;
import com.rvmc.ordertaker.dto.LoginRequest;
import com.rvmc.ordertaker.dto.RestaurantOwnerRegistrationRequest;
import com.rvmc.ordertaker.exception.DuplicateEmailException;
import com.rvmc.ordertaker.exception.GlobalExceptionHandler;
import com.rvmc.ordertaker.exception.InvalidCredentialsException;
import com.rvmc.ordertaker.service.AuthService;
import com.rvmc.ordertaker.security.JwtService;
import com.rvmc.ordertaker.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuthService authService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void registerCustomer_shouldReturnCreatedTokenResponse() throws Exception {
        when(authService.registerCustomer(any(CustomerRegistrationRequest.class))).thenReturn(
                new AuthResponse("customer-token", "Bearer", 7L, "customer@example.com", "CUSTOMER")
        );

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "customer",
                                  "email": "customer@example.com",
                                  "password": "password123",
                                  "customerName": "Customer Name",
                                  "address1": "Address 1",
                                  "address2": "Address 2"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("customer-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.email").value("customer@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void registerRestaurantOwner_shouldReturnCreatedTokenResponse() throws Exception {
        when(authService.registerRestaurantOwner(any(RestaurantOwnerRegistrationRequest.class))).thenReturn(
                new AuthResponse("owner-token", "Bearer", 12L, "owner@example.com", "RESTAURANT_OWNER")
        );

        mockMvc.perform(post("/api/v1/auth/register/restaurant-owner")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "owner",
                                  "email": "owner@example.com",
                                  "password": "password123",
                                  "restaurantName": "Pizza Place",
                                  "address1": "Address 1",
                                  "address2": "Address 2"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("owner-token"))
                .andExpect(jsonPath("$.role").value("RESTAURANT_OWNER"));
    }

    @Test
    void registerCustomer_shouldReturnConflict_whenEmailIsDuplicate() throws Exception {
        when(authService.registerCustomer(any(CustomerRegistrationRequest.class)))
                .thenThrow(new DuplicateEmailException("customer@example.com"));

        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "customer",
                                  "email": "customer@example.com",
                                  "password": "password123",
                                  "customerName": "Customer Name"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(
                        "Email is already registered: customer@example.com"
                ));
    }

    @Test
    void registerCustomer_shouldReturnBadRequest_whenRequestIsInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register/customer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "",
                                  "email": "not-an-email",
                                  "password": "short",
                                  "customerName": ""
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        when(authService.login(any(LoginRequest.class))).thenReturn(
                new AuthResponse("login-token", "Bearer", 7L, "customer@example.com", "CUSTOMER")
        );

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "customer@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("login-token"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void login_shouldReturnUnauthorized_whenCredentialsAreInvalid() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new InvalidCredentialsException());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "customer@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password"));
    }
}
