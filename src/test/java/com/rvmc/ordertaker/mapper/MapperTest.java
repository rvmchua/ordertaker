package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.entity.*;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapperTest {
    private final MenuItemMapper menuItemMapper = Mappers.getMapper(MenuItemMapper.class);
    private final OrderMapper orderMapper = Mappers.getMapper(OrderMapper.class);
    private final RestaurantMapper restaurantMapper = Mappers.getMapper(RestaurantMapper.class);

    @Test
    void menuItemMapper_shouldCreateActiveNonDeletedEntity() {
        MenuItemRequest request = new MenuItemRequest(
                5L, "Burger", "Description", BigDecimal.valueOf(250), true, true
        );

        MenuItem item = menuItemMapper.fromRequestToEntity(request);

        assertEquals("Burger", item.getItemName());
        assertTrue(item.getIsAvailable());
        assertFalse(item.getIsDeleted());
    }

    @Test
    void menuItemMapper_shouldUpdateAndMapResponse() {
        MenuItem item = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(Restaurant.builder().restaurantId(5L).build())
                .itemName("Old")
                .description("Old")
                .price(BigDecimal.valueOf(100))
                .isAvailable(true)
                .isDeleted(false)
                .build();
        MenuItemRequest request = new MenuItemRequest(
                null, "New", null, BigDecimal.valueOf(150), null, null
        );

        menuItemMapper.updateEntityFromRequest(request, item);
        MenuItemResponse response = menuItemMapper.toResponse(item);

        assertEquals("New", item.getItemName());
        assertEquals(BigDecimal.valueOf(150), item.getPrice());
        assertEquals("Old", item.getDescription());
        assertEquals("New", response.itemName());
        assertEquals(5L, response.restaurantId());
    }

    @Test
    void orderMapper_shouldMapOrderIdsStatusAndTotal() {
        Order order = Order.builder()
                .orderId(100L)
                .restaurant(Restaurant.builder().restaurantId(5L).build())
                .customer(Customer.builder().customerId(1L).build())
                .status(OrderStatus.PENDING)
                .totalAmount(BigDecimal.valueOf(300))
                .orderItems(Collections.emptyList())
                .build();

        OrderResponse response = orderMapper.toResponse(order);

        assertEquals(100L, response.orderId());
        assertEquals(5L, response.restaurantId());
        assertEquals(1L, response.customerId());
        assertEquals(OrderStatus.PENDING, response.status());
    }

    @Test
    void restaurantMapper_shouldMapRestaurantProfile() {
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(5L)
                .restaurantName("Bistro")
                .address1("Address 1")
                .address2("Address 2")
                .menuItemList(Collections.emptyList())
                .build();

        RestaurantProfileResponse response =
                restaurantMapper.toRestaurantProfileResponse(restaurant);

        assertEquals(5L, response.restaurantId());
        assertEquals("Bistro", response.restaurantName());
        assertEquals("Address 1", response.address1());
        assertTrue(response.menuItemList().isEmpty());
    }
}
