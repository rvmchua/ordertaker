package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
    List<Restaurant> findAllByRestaurantNameContainingIgnoreCase(String restaurantName);
}
