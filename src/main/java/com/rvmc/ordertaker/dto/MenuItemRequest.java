package com.rvmc.ordertaker.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MenuItemRequest(
        @NotNull
        Long restaurantId,

        @NotBlank(message = "Item name cannot be blank")
        String itemName,
        String description,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "1.00", inclusive = true, message = "Price must be at least Php 1.00")
        BigDecimal price,
        Boolean isAvailable,
        Boolean isDeleted
) {
}
