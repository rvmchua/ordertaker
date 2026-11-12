package com.rvmc.ordertaker.dto;

import java.math.BigDecimal;

public record MenuItemResponse(
        String menuItemId,
        String restaurantId,
        String itemName,
        String description,
        BigDecimal price,
        Boolean isAvailable,
        Boolean isDeleted
) {
}
