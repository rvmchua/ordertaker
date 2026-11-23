package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.*;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.entity.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:repository-test;MODE=PostgreSQL",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class RepositoryTest {
    @Autowired private RestaurantRepository restaurantRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private CartRepository cartRepository;
    @Autowired private OrderRepository orderRepository;
    @Autowired private CustomerRepository customerRepository;
    @Autowired private UserRepository userRepository;

    private Customer customer;
    private Restaurant restaurant;

    @BeforeEach
    void setUp() {
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        menuItemRepository.deleteAll();
        restaurantRepository.deleteAll();
        customerRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(User.builder()
                .username("owner")
                .email("owner@example.com")
                .passwordHash("hash")
                .role(Role.RESTAURANT)
                .build());
        restaurant = restaurantRepository.save(Restaurant.builder()
                .restaurantName("Pizza Palace")
                .address1("Address 1")
                .address2("Address 2")
                .owner(owner)
                .isActive(true)
                .toDelete(false)
                .build());

        User customerUser = userRepository.save(User.builder()
                .username("customer")
                .email("customer@example.com")
                .passwordHash("hash")
                .role(Role.CUSTOMER)
                .build());
        customer = customerRepository.save(Customer.builder()
                .customerName("Royce")
                .address1("Address 1")
                .address2("Address 2")
                .user(customerUser)
                .isActive(true)
                .build());
    }

    @Test
    void restaurantRepository_shouldFindByNameIgnoringCase() {
        assertEquals(
                1,
                restaurantRepository.findAllByRestaurantNameContainingIgnoreCase("pizza").size()
        );
    }

    @Test
    void menuItemRepository_shouldFilterDeletedAndRestaurantMenuItems() {
        MenuItem active = menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant)
                .itemName("Pizza")
                .price(BigDecimal.valueOf(300))
                .isAvailable(true)
                .isDeleted(false)
                .build());
        menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant)
                .itemName("Old Pizza")
                .price(BigDecimal.valueOf(250))
                .isAvailable(false)
                .isDeleted(true)
                .build());

        assertEquals(
                1,
                menuItemRepository.findByRestaurantAndIsDeleted(restaurant, false).size()
        );
        assertTrue(menuItemRepository
                .findByMenuItemIdAndIsDeleted(active.getMenuItemId(), false)
                .isPresent());
        assertEquals(
                1,
                menuItemRepository.findAllByMenuItemIdInAndIsDeleted(
                        java.util.List.of(active.getMenuItemId()), false
                ).size()
        );
    }

    @Test
    void cartAndOrderRepositories_shouldFindActiveCartAndOwnedOrder() {
        Cart cart = cartRepository.save(Cart.builder()
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .build());

        Order order = orderRepository.save(Order.builder()
                .customer(customer)
                .restaurant(restaurant)
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(300))
                .build());

        assertTrue(cartRepository.findByCustomerAndIsActive(customer, true).isPresent());
        assertTrue(cartRepository.existsByCustomerAndIsActive(customer, true));
        assertTrue(orderRepository.findByOrderIdAndCustomer(order.getOrderId(), customer).isPresent());
        assertTrue(orderRepository.findByOrderIdAndRestaurant(order.getOrderId(), restaurant).isPresent());
        assertEquals(1, orderRepository.findAllByCustomer(customer).size());
        assertEquals(1, orderRepository.findAllByRestaurant(restaurant).size());
        assertTrue(orderRepository.findByOrderIdAndStatusNotIn(
                order.getOrderId(), java.util.List.of(OrderStatus.CANCELLED)
        ).isPresent());
    }

    @Test
    void cartRepository_shouldNotFindCart_whenCartIsInactive() {
        cartRepository.save(Cart.builder()
                .customer(customer)
                .restaurant(restaurant)
                .isActive(false)
                .build());

        assertTrue(cartRepository.findByCustomerAndIsActive(customer, true).isEmpty());
        assertTrue(!cartRepository.existsByCustomerAndIsActive(customer, true));
    }

    @Test
    void menuItemRepository_shouldExcludeDeletedAndMissingMenuItems() {
        menuItemRepository.save(MenuItem.builder()
                .restaurant(restaurant)
                .itemName("Deleted Pizza")
                .price(BigDecimal.valueOf(250))
                .isAvailable(false)
                .isDeleted(true)
                .build());

        assertTrue(menuItemRepository.findByMenuItemIdAndIsDeleted(999L, false).isEmpty());
        assertTrue(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(
                java.util.List.of(999L), false
        ).isEmpty());
    }
}
