package com.rvmc.ordertaker.dto;

import com.rvmc.ordertaker.entity.enums.OrderStatus;

public record UpdateOrderStatus(
        OrderStatus status
) {
}
