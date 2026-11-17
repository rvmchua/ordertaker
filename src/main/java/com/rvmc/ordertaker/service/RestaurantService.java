package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.RestaurantMapper;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class RestaurantService {
    private final RestaurantRepository restaurantRepository;
    private final RestaurantMapper restaurantMapper;

    public List<RestaurantProfileResponse> getAllRestaurants() {
        return restaurantRepository
                .findAll()
                .stream()
                .map(restaurantMapper::toRestaurantProfileResponse)
                .toList();
    }

    public RestaurantProfileResponse getRestaurantById(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        return restaurantMapper.toRestaurantProfileResponse(restaurant);
    }

    public List<RestaurantProfileResponse> getRestaurantByQuery(String request) {
        List<Restaurant> restaurants = restaurantRepository.findAllByRestaurantNameContainingIgnoreCase(request.trim());

        return restaurants
                .stream().map(restaurantMapper::toRestaurantProfileResponse)
                .toList();
    }
}
