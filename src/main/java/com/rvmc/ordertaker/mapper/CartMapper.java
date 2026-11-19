package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.CartResponse;
import com.rvmc.ordertaker.dto.UpdateCartRequest;
import com.rvmc.ordertaker.dto.CreateCartRequest;
import com.rvmc.ordertaker.entity.Cart;
import com.rvmc.ordertaker.entity.CartItem;
import com.rvmc.ordertaker.entity.MenuItem;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CartMapper {

    CartItem toCartItem(MenuItem menuItem);

    CartResponse toResponse(Cart cart);

    Cart fromCartRequestToEntity(UpdateCartRequest request);

    Cart fromCartRequestToEntity(CreateCartRequest request);
}
