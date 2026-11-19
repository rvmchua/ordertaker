package com.rvmc.ordertaker.dto;

public record CartItemUpdateRequest(
        Long menuItemId,
        Short quantity
) {
}
