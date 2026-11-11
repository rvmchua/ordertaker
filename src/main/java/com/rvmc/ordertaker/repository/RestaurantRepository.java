package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantRepository extends JpaRepository<Restaurant, Long> {
}
