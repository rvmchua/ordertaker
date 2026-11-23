package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Order;
import com.rvmc.ordertaker.entity.OrderItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderItem toOrderItem(MenuItem menuItem);

    @Mapping(source = "restaurant.restaurantId", target = "restaurantId")
    @Mapping(source = "customer.customerId", target = "customerId")
    OrderResponse toResponse(Order order);
}
