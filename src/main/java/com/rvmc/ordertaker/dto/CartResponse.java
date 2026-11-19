package com.rvmc.ordertaker.dto;

import com.rvmc.ordertaker.entity.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        Long cartId,
        Long restaurantId,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        List<CartItemResponse> cartItems
) {
}
