package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.Order;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.entity.enums.Role;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.OrderRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import com.rvmc.ordertaker.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:order-status-api-test;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class OrderStatusApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    private Customer customer;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        restaurantRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        User customerUser = userRepository.save(User.builder()
                .username("status-customer")
                .email("status-customer@example.com")
                .passwordHash("hash")
                .role(Role.CUSTOMER)
                .build());
        customer = customerRepository.save(Customer.builder()
                .customerName("Status Customer")
                .address1("Address 1")
                .address2("Address 2")
                .user(customerUser)
                .isActive(true)
                .build());

        User owner = userRepository.save(User.builder()
                .username("status-owner")
                .email("status-owner@example.com")
                .passwordHash("hash")
                .role(Role.RESTAURANT)
                .build());
        restaurant = restaurantRepository.save(Restaurant.builder()
                .restaurantName("Status Bistro")
                .address1("Address 1")
                .address2("Address 2")
                .owner(owner)
                .isActive(true)
                .toDelete(false)
                .build());
    }

    @Test
    void updateOrderStatus_shouldConfirmSeededOrder_whenRestaurantTransitionIsValid() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(300))
                .build());

        mockMvc.perform(patch("/api/v1/restaurants/order/" + order.getOrderId())
                        .param("restaurantId", restaurant.getRestaurantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(order.getOrderId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        assertEquals(
                OrderStatus.CONFIRMED,
                orderRepository.findById(order.getOrderId()).orElseThrow().getStatus()
        );
    }

    @Test
    void updateOrderStatus_shouldRejectSeededOrder_whenRestaurantTransitionIsInvalid() throws Exception {
        Order order = orderRepository.save(Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(300))
                .build());

        mockMvc.perform(patch("/api/v1/restaurants/order/" + order.getOrderId())
                        .param("restaurantId", restaurant.getRestaurantId().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"FOR_PICKUP\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error")
                        .value("Restaurant cannot change order status from PENDING to FOR_PICKUP."));

        assertEquals(
                OrderStatus.PENDING,
                orderRepository.findById(order.getOrderId()).orElseThrow().getStatus()
        );
    }
}
