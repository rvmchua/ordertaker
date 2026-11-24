package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.AuthResponse;
import com.rvmc.ordertaker.dto.CustomerRegistrationRequest;
import com.rvmc.ordertaker.dto.LoginRequest;
import com.rvmc.ordertaker.dto.RestaurantOwnerRegistrationRequest;
import com.rvmc.ordertaker.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Registers a customer and returns an access token.
     *
     * @param request customer registration details
     * @return the registered customer identity and signed JWT
     */
    @PostMapping("/register/customer")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerCustomer(@Valid @RequestBody CustomerRegistrationRequest request) {
        return authService.registerCustomer(request);
    }

    /**
     * Registers a restaurant owner and returns an access token.
     *
     * @param request restaurant-owner registration details
     * @return the registered owner identity and signed JWT
     */
    @PostMapping("/register/restaurant-owner")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse registerRestaurantOwner(
            @Valid @RequestBody RestaurantOwnerRegistrationRequest request
    ) {
        return authService.registerRestaurantOwner(request);
    }

    /**
     * Authenticates an email and password pair.
     *
     * @param request login credentials
     * @return the authenticated identity and signed JWT
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
