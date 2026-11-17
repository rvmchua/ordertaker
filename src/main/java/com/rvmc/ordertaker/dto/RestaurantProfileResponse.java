package com.rvmc.ordertaker.dto;

import java.util.List;

public record RestaurantProfileResponse(
        Long restaurantId,
        String restaurantName,
        String address1,
        String address2,
        List<MenuItemResponse> menuItemList
) {
}
