package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.dto.RestaurantProfileRequest;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.exception.ResourceAlreadyTaggedAsDeletedException;
import com.rvmc.ordertaker.exception.DuplicateEmailException;
import com.rvmc.ordertaker.mapper.RestaurantMapper;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import com.rvmc.ordertaker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;
    private final UserRepository userRepository;

    /**
     * Lists every restaurant.
     *
     * @return mapped restaurant profiles
     */
    public List<RestaurantProfileResponse> getAllRestaurants() {
        return restaurantRepository
                .findAll()
                .stream()
                .filter(restaurant -> !Boolean.TRUE.equals(restaurant.getToDelete()))
                .filter(restaurant -> Boolean.TRUE.equals(restaurant.getIsActive()))
                .map(restaurantMapper::toRestaurantProfileResponse)
                .toList();
    }

    /**
     * Finds a restaurant by identifier.
     *
     * @param restaurantId restaurant identifier
     * @return mapped restaurant profile
     */
    public RestaurantProfileResponse getRestaurantById(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (Boolean.TRUE.equals(restaurant.getToDelete())) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }
        if (!Boolean.TRUE.equals(restaurant.getIsActive())) {
            throw new ResourceNotFoundException("Restaurant", restaurantId);
        }

        return restaurantMapper.toRestaurantProfileResponse(restaurant);
    }

    /**
     * Searches restaurants by a case-insensitive name fragment.
     *
     * @param request restaurant-name fragment
     * @return matching mapped restaurant profiles
     */
    public List<RestaurantProfileResponse> getRestaurantByQuery(String request) {
        List<Restaurant> restaurants = restaurantRepository.findAllByRestaurantNameContainingIgnoreCase(request.trim());

        return restaurants
                .stream()
                .filter(restaurant -> !Boolean.TRUE.equals(restaurant.getToDelete()))
                .filter(restaurant -> Boolean.TRUE.equals(restaurant.getIsActive()))
                .map(restaurantMapper::toRestaurantProfileResponse)
                .toList();
    }

    public RestaurantProfileResponse findProfile(Long restaurantId) {
        return getRestaurantById(restaurantId);
    }

    @Transactional
    public RestaurantProfileResponse editProfile(Long restaurantId, RestaurantProfileRequest request) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (Boolean.TRUE.equals(restaurant.getToDelete())) {
            throw new ResourceAlreadyTaggedAsDeletedException(restaurantId, "Restaurant");
        }

        if (request.restaurantName() != null) {
            restaurant.setRestaurantName(request.restaurantName());
        }
        if (request.address1() != null) {
            restaurant.setAddress1(request.address1());
        }
        if (request.address2() != null) {
            restaurant.setAddress2(request.address2());
        }
        if (request.username() != null || request.email() != null) {
            if (restaurant.getOwner() == null) {
                throw new IllegalStateException("Restaurant account is missing its owner identity.");
            }
            if (request.username() != null) {
                restaurant.getOwner().setUsername(request.username());
            }
            if (request.email() != null) {
                String email = request.email().trim().toLowerCase(Locale.ROOT);
                if (userRepository.existsByEmailAndUserIdNot(email, restaurant.getOwner().getUserId())) {
                    throw new DuplicateEmailException(email);
                }
                restaurant.getOwner().setEmail(email);
            }
        }
        return restaurantMapper.toRestaurantProfileResponse(restaurantRepository.save(restaurant));
    }

    @Transactional
    public void deleteProfile(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (Boolean.TRUE.equals(restaurant.getToDelete())) {
            throw new ResourceAlreadyTaggedAsDeletedException(restaurantId, "Restaurant");
        }
        restaurant.setToDelete(true);
        restaurant.setIsActive(false);
        restaurantRepository.save(restaurant);
    }

    @Transactional
    public void hardDeleteRestaurantByAdmin(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        if (restaurant.getOwner() == null) {
            restaurantRepository.delete(restaurant);
            return;
        }
        userRepository.delete(restaurant.getOwner());
    }

    @Transactional
    public RestaurantProfileResponse reactivateRestaurantByAdmin(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        restaurant.setToDelete(false);
        restaurant.setIsActive(true);
        restaurant = restaurantRepository.save(restaurant);
        return restaurantMapper.toRestaurantProfileResponse(restaurant);
    }
}
