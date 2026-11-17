package com.rvmc.ordertaker.dto;

import java.math.BigDecimal;

public record MenuItemResponse(
        Long menuItemId,
        Long restaurantId,
        String itemName,
        String description,
        BigDecimal price,
        Boolean isAvailable,
        Boolean isDeleted
) {
}
