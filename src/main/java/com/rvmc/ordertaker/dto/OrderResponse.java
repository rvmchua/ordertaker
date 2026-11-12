package com.rvmc.ordertaker.dto;

import com.rvmc.ordertaker.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record OrderResponse(
        Long orderId,
        Long restaurantId,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemResponse> orderItems
) {
}
