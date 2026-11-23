package com.rvmc.ordertaker.entity;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CartTest {
    @Test
    void create_shouldInitializeAnActiveCart() {
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();
        Customer customer = Customer.builder().customerId(1L).build();

        Cart cart = Cart.create(restaurant, customer);

        assertSame(restaurant, cart.getRestaurant());
        assertSame(customer, cart.getCustomer());
        assertTrue(cart.getIsActive());
        assertTrue(cart.getCartItems().isEmpty());
    }

    @Test
    void addCartItem_shouldSetBothSidesOfRelationship() {
        Cart cart = Cart.builder().cartItems(new ArrayList<>()).build();
        CartItem item = CartItem.builder().quantity((short) 2).build();

        cart.addCartItem(item);

        assertEquals(List.of(item), cart.getCartItems());
        assertSame(cart, item.getCart());
    }

    @Test
    void replaceCartItems_shouldReplaceItemsAndSetCartOnNewItems() {
        CartItem existing = CartItem.builder().quantity((short) 1).build();
        Cart cart = Cart.builder()
                .cartItems(new java.util.ArrayList<>(List.of(existing)))
                .build();
        CartItem replacement = CartItem.builder().quantity((short) 3).build();

        cart.replaceCartItems(List.of(replacement));

        assertEquals(List.of(replacement), cart.getCartItems());
        assertSame(cart, replacement.getCart());
    }
}
