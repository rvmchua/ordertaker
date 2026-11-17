package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.entity.Restaurant;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface RestaurantMapper {
    RestaurantProfileResponse toRestaurantProfileResponse(Restaurant restaurant);
}
