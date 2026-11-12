package com.rvmc.ordertaker.mapper;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.entity.MenuItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MenuItemMapper {

    @Mapping(target = "menuItemId", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    @Mapping(target = "isAvailable", constant = "true")
    @Mapping(target = "isDeleted", constant = "false")
    MenuItem fromRequestToEntity(MenuItemRequest request);

    MenuItemResponse toResponse(MenuItem savedMenu);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "menuItemId", ignore = true)
    @Mapping(target = "restaurant", ignore = true)
    void updateEntityFromRequest(MenuItemRequest request, @MappingTarget MenuItem existingItem);
}
