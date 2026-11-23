package com.rvmc.ordertaker.dto;

import com.rvmc.ordertaker.entity.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatus(
        @NotNull
        OrderStatus status
) {
}
