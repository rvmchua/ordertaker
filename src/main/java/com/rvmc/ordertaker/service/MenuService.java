package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.exception.ResourceAlreadyTaggedAsDeletedException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.MenuItemMapper;
import com.rvmc.ordertaker.repository.MenuItemRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Getter
@Setter
@RequiredArgsConstructor
public class MenuService {
    private final MenuItemMapper menuItemMapper;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    /*
     *   Basic CRUD for menu items
     */
    public List<MenuItemResponse> getMenuFromRestaurant(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));

        return menuItemRepository
                .findByRestaurantAndIsDeleted(restaurant, false)
                .stream()
                .map(menuItemMapper::toResponse)
                .toList();
    }

    public MenuItemResponse getMenuItemById(Long menuItemId) {
        return menuItemMapper.toResponse(menuItemRepository.findByMenuItemIdAndIsDeleted(menuItemId, false)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId)));
    }

    @Transactional
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", request.restaurantId()));
        MenuItem menuItem = menuItemMapper.fromRequestToEntity(request);
        menuItem.setRestaurant(restaurant);
        MenuItem savedMenu = menuItemRepository.save(menuItem);

        return menuItemMapper.toResponse(savedMenu);
    }

    @Transactional
    public MenuItemResponse updateMenuItem(Long menuItemId, MenuItemRequest request) {
        MenuItem existingItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId));
        menuItemMapper.updateEntityFromRequest(request, existingItem);
        MenuItem updatedItem = menuItemRepository.save(existingItem);

        return menuItemMapper.toResponse(updatedItem);
    }

    @Transactional
    public MenuItemResponse updateItemAvailability(Long menuItemId) {
        MenuItem existingItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId));
        existingItem.setIsAvailable(!existingItem.getIsAvailable());
        MenuItem flippedAvailability = menuItemRepository.save(existingItem);

        return menuItemMapper.toResponse(flippedAvailability);
    }

    @Transactional
    public void deleteMenuItemSoft(Long menuItemId) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId));
        if (Boolean.TRUE.equals(item.getIsDeleted())) {
            throw new ResourceAlreadyTaggedAsDeletedException(menuItemId, "Menu item");
        }
        item.setIsDeleted(true);
        menuItemRepository.save(item);
    }
}
