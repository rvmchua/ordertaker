package com.rvmc.ordertaker.entity;

import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {
    @Test
    void create_shouldSnapshotPricesCalculateTotalAndLinkOrderItems() {
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();
        Customer customer = Customer.builder().customerId(1L).build();
        MenuItem item = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(150.00))
                .isAvailable(true)
                .build();
        Cart cart = Cart.builder()
                .restaurant(restaurant)
                .customer(customer)
                .cartItems(List.of(CartItem.builder().menuItem(item).quantity((short) 2).build()))
                .build();

        Order order = Order.create(cart);

        assertEquals(BigDecimal.valueOf(300.00), order.getTotalAmount());
        assertEquals(1, order.getOrderItems().size());
        assertEquals(BigDecimal.valueOf(150.00), order.getOrderItems().getFirst().getPriceAtOrderTime());
        assertEquals(order, order.getOrderItems().getFirst().getOrder());
    }

    @Test
    void create_shouldRejectEmptyCart() {
        Cart cart = Cart.builder()
                .restaurant(Restaurant.builder().restaurantId(5L).build())
                .customer(Customer.builder().customerId(1L).build())
                .cartItems(List.of())
                .build();

        assertThrows(IllegalStateException.class, () -> Order.create(cart));
    }

    @Test
    void create_shouldRejectUnavailableMenuItem() {
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();
        Customer customer = Customer.builder().customerId(1L).build();
        MenuItem item = MenuItem.builder()
                .price(BigDecimal.valueOf(150.00))
                .isAvailable(false)
                .build();
        Cart cart = Cart.builder()
                .restaurant(restaurant)
                .customer(customer)
                .cartItems(List.of(CartItem.builder().menuItem(item).quantity((short) 1).build()))
                .build();

        assertThrows(ResourceNotFoundException.class, () -> Order.create(cart));
    }
}
