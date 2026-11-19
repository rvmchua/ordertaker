package com.rvmc.ordertaker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateCartRequest(
        @NotNull
        Long customerId,
        Long restaurantId,
        @NotNull
        List<@NotNull @Valid CartItemUpdateRequest> cartItems
) {
}
