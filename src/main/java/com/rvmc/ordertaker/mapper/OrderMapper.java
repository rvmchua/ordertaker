package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Order;
import com.rvmc.ordertaker.entity.OrderItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderItem toOrderItem(MenuItem menuItem);

    OrderResponse toResponse(Order order);
}
