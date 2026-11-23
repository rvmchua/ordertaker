package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.CartResponse;
import com.rvmc.ordertaker.entity.Cart;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {
    @Mapping(source = "restaurant.restaurantId", target = "restaurantId")
    @Mapping(source = "customer.customerId", target = "customerId")
    CartResponse toResponse(Cart cart);
}
