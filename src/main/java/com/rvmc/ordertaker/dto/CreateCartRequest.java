package com.rvmc.ordertaker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateCartRequest(
        @NotNull(message = "Customer ID is required")
        Long customerId,

        @NotNull(message = "Restaurant ID is required")
        Long restaurantId,

        @NotEmpty(message = "Order must contain at least one item")
        List<@NotNull @Valid CartItemRequest> cartItems
) {
}
