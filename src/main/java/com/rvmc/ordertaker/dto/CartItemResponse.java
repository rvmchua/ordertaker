package com.rvmc.ordertaker.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long cartItemId,
        Long cartId,
        Short quantity,
        BigDecimal price
) {
}
