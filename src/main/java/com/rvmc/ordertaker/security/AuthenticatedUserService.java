package com.rvmc.ordertaker.security;

import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.User;
import com.rvmc.ordertaker.exception.ForbiddenException;
import com.rvmc.ordertaker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticatedUserService {
    private final UserRepository userRepository;

    /**
     * Resolves the customer profile associated with the authenticated principal.
     *
     * @param authentication Spring Security authentication
     * @return the authenticated customer's profile
     * @throws ForbiddenException if authentication is missing or the user is not a customer
     */
    public Customer getCustomer(Authentication authentication) {
        User user = getUser(authentication);
        if (user.getCustomer() == null) {
            throw new ForbiddenException("Authenticated user is not a customer.");
        }
        return user.getCustomer();
    }

    /**
     * Resolves the restaurant owned by the authenticated principal.
     *
     * @param authentication Spring Security authentication
     * @return the owned restaurant
     * @throws ForbiddenException if authentication is missing or the user is not an owner
     */
    public Restaurant getRestaurant(Authentication authentication) {
        User user = getUser(authentication);
        if (user.getRestaurant() == null) {
            throw new ForbiddenException("Authenticated user is not a restaurant owner.");
        }
        return user.getRestaurant();
    }

    private User getUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new ForbiddenException("Authentication is required.");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ForbiddenException("Authenticated user does not exist."));
    }
}
