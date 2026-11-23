package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.RestaurantMapper;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    /**
     * Lists every restaurant.
     *
     * @return mapped restaurant profiles
     */
    public List<RestaurantProfileResponse> getAllRestaurants() {
        return restaurantRepository
                .findAll()
                .stream()
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
                .stream().map(restaurantMapper::toRestaurantProfileResponse)
                .toList();
    }
}
