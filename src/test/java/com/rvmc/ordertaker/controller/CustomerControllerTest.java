package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.CartResponse;
import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.UpdateOrderStatus;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.exception.GlobalExceptionHandler;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.service.CartService;
import com.rvmc.ordertaker.service.CustomerService;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import com.rvmc.ordertaker.security.AuthenticatedUserService;
import com.rvmc.ordertaker.security.JwtService;
import com.rvmc.ordertaker.repository.UserRepository;
import com.rvmc.ordertaker.entity.Customer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(GlobalExceptionHandler.class)
class CustomerControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private CustomerService customerService;
    @MockitoBean private MenuService menuService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private CartService cartService;
    @MockitoBean private AuthenticatedUserService authenticatedUserService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        Customer customer = Customer.builder().customerId(1L).build();
        when(authenticatedUserService.getCustomer(null)).thenReturn(customer);
        when(authenticatedUserService.getCustomer(
                org.mockito.ArgumentMatchers.any(org.springframework.security.core.Authentication.class)
        )).thenReturn(customer);
    }

    @Test
    void getActiveCart_shouldReturnCartResponse() throws Exception {
        CartResponse response = new CartResponse(
                20L, 5L, 1L, OrderStatus.PENDING, BigDecimal.ZERO, List.of()
        );
        when(cartService.getActiveCart(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cartId").value(20))
                .andExpect(jsonPath("$.customerId").value(1))
                .andExpect(jsonPath("$.restaurantId").value(5));
    }

    @Test
    void cancelOrder_shouldRejectNonCancelledStatus() throws Exception {
        mockMvc.perform(patch("/api/v1/customers/order/100")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cancelOrder_shouldReturnCancelledOrder_whenStatusIsCancelled() throws Exception {
        UpdateOrderStatus request = new UpdateOrderStatus(OrderStatus.CANCELLED);
        OrderResponse response = new OrderResponse(
                100L, 5L, 1L, OrderStatus.CANCELLED, BigDecimal.valueOf(300), List.of()
        );
        when(orderService.cancelOrderByCustomer(
                eq(request), eq(100L), eq(1L)
        )).thenReturn(response);

        mockMvc.perform(patch("/api/v1/customers/order/100")
                        .param("customerId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(100))
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void getActiveCart_shouldReturnNotFound_whenCartDoesNotExist() throws Exception {
        when(cartService.getActiveCart(1L))
                .thenThrow(new ResourceNotFoundException("No active cart found."));

        mockMvc.perform(get("/api/v1/customers/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No active cart found."));
    }

    @Test
    void updateCart_shouldReturnBadRequest_whenCustomerIdIsMissing() throws Exception {
        mockMvc.perform(put("/api/v1/customers/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customerId\":null,\"restaurantId\":5,\"cartItems\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateProfile_shouldReturnBadRequest_whenEmailIsInvalid() throws Exception {
        mockMvc.perform(put("/api/v1/customers/profile")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.email").exists());
    }
}
