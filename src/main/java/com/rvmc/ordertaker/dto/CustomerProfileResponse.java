package com.rvmc.ordertaker.dto;

public record CustomerProfileResponse(
        String username,
        String email,
        String customerName,
        String address1,
        String address2
) {
}
