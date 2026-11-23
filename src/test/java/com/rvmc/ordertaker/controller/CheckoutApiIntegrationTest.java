package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.entity.*;
import com.rvmc.ordertaker.entity.enums.Role;
import com.rvmc.ordertaker.repository.*;
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
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:checkout-api-test;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class CheckoutApiIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private UserRepository userRepository;

    private Customer customer;
    private Restaurant restaurant;
    private MenuItem menuItem;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        cartRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        User customerUser = userRepository.save(User.builder()
                .username("checkout-customer")
                .email("checkout-customer@example.com")
                .passwordHash("hash")
                .role(Role.CUSTOMER)
                .build());
        customer = customerRepository.save(Customer.builder()
                .customerName("Checkout Customer")
                .address1("Address 1")
                .address2("Address 2")
                .user(customerUser)
                .isActive(true)
                .build());

        User owner = userRepository.save(User.builder()
                .username("checkout-owner")
                .email("checkout-owner@example.com")
                .passwordHash("hash")
                .role(Role.RESTAURANT)
                .build());
        restaurant = restaurantRepository.save(Restaurant.builder()
                .restaurantName("Checkout Bistro")
                .address1("Address 1")
                .address2("Address 2")
                .owner(owner)
                .isActive(true)
                .toDelete(false)
                .build());

        menuItem = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant)
                .itemName("Checkout Burger")
                .description("Test burger")
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build());
    }

    @Test
    void checkoutCart_shouldCreateOrderSnapshotPriceAndDeactivateCart_whenCartIsValid() throws Exception {
        Cart cart = createActiveCart((short) 2);

        mockMvc.perform(post("/api/v1/customers/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customer.getCustomerId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(customer.getCustomerId()))
                .andExpect(jsonPath("$.restaurantId").value(restaurant.getRestaurantId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalAmount").value(500.00))
                .andExpect(jsonPath("$.orderItems.length()").value(1))
                .andExpect(jsonPath("$.orderItems[0].quantity").value(2))
                .andExpect(jsonPath("$.orderItems[0].priceAtOrderTime").value(250.00));

        Cart savedCart = cartRepository.findById(cart.getCartId()).orElseThrow();
        assertFalse(savedCart.getIsActive());

        assertEquals(1, orderRepository.findAllByCustomer(customer).size());
        Order savedOrder = orderRepository.findAllByCustomer(customer).getFirst();
        assertEquals(0, BigDecimal.valueOf(500.00).compareTo(savedOrder.getTotalAmount()));
        assertEquals(BigDecimal.valueOf(250.00),
                savedOrder.getOrderItems().getFirst().getPriceAtOrderTime());
        assertNotNull(savedOrder.getOrderItems().getFirst().getOrder());
    }

    @Test
    void checkoutCart_shouldReturnBadRequest_whenActiveCartIsEmpty() throws Exception {
        Cart cart = Cart.create(restaurant, customer);
        cart.setCartItems(new ArrayList<>());
        cartRepository.save(cart);

        mockMvc.perform(post("/api/v1/customers/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customer.getCustomerId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Cannot create an order from an empty cart."));

        assertTrue(orderRepository.findAllByCustomer(customer).isEmpty());
    }

    @Test
    void checkoutCart_shouldReturnError_whenMenuItemIsUnavailable() throws Exception {
        menuItem.setIsAvailable(false);
        menuItemRepository.save(menuItem);
        createActiveCart((short) 1);

        mockMvc.perform(post("/api/v1/customers/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customer.getCustomerId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Menu item is currently unavailable."));

        assertTrue(orderRepository.findAllByCustomer(customer).isEmpty());
        Cart cart = cartRepository.findByCustomerAndIsActive(customer, true).orElseThrow();
        assertTrue(cart.getIsActive());
    }

    @Test
    void checkoutCart_shouldReturnNotFound_whenCustomerDoesNotExist() throws Exception {
        mockMvc.perform(post("/api/v1/customers/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Customer not found with id: 999"));
    }

    @Test
    void checkoutCart_shouldReturnNotFound_whenCustomerHasNoActiveCart() throws Exception {
        mockMvc.perform(post("/api/v1/customers/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(customer.getCustomerId().toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("No active cart found."));
    }

    private Cart createActiveCart(short quantity) {
        Cart cart = Cart.create(restaurant, customer);
        cart.addCartItem(CartItem.builder()
                .menuItem(menuItem)
                .quantity(quantity)
                .build());
        return cartRepository.save(cart);
    }
}
