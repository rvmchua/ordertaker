package com.rvmc.ordertaker.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RestaurantSignupRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Email @Size(max = 320) String email,
        @NotBlank String password,
        @NotBlank @Size(max = 255) String address1,
        @Size(max = 255) String address2,
        @NotBlank @Size(max = 255) String restaurantName
) {
    public RestaurantSignupRequest {
        email = email == null ? null : email.trim();
    }
}
