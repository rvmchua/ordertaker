package com.rvmc.ordertaker.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record CustomerProfileRequest(
        String username,
        @Email @Size(min = 1) String email,
        String customerName,
        String address1,
        String address2
) {
}
