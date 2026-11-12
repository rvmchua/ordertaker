package com.rvmc.ordertaker.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long orderItemId,
        Long orderId,
        Short quantity,
        BigDecimal priceAtOrderTime
) {
}
