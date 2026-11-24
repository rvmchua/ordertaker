package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.exception.ResourceAlreadyTaggedAsDeletedException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.exception.ForbiddenException;
import com.rvmc.ordertaker.mapper.MenuItemMapper;
import com.rvmc.ordertaker.repository.MenuItemRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuService {
    private final MenuItemMapper menuItemMapper;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;

    /**
     * Lists active, non-deleted menu items for a restaurant.
     *
     * @param restaurantId restaurant identifier
     * @return mapped active menu items
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

    /**
     * Finds one active, non-deleted menu item.
     *
     * @param menuItemId menu-item identifier
     * @return mapped menu item
     */
    public MenuItemResponse getMenuItemById(Long menuItemId) {
        return menuItemMapper.toResponse(menuItemRepository.findByMenuItemIdAndIsDeleted(menuItemId, false)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId)));
    }

    /**
     * Creates a menu item for an existing restaurant.
     *
     * @param request menu-item creation request
     * @return the mapped saved menu item
     */
    @Transactional
    public MenuItemResponse createMenuItem(MenuItemRequest request) {
        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", request.restaurantId()));
        MenuItem menuItem = menuItemMapper.fromRequestToEntity(request);
        menuItem.setRestaurant(restaurant);
        MenuItem savedMenu = menuItemRepository.save(menuItem);

        return menuItemMapper.toResponse(savedMenu);
    }

    /**
     * Updates mutable fields on an existing menu item.
     *
     * @param menuItemId menu-item identifier
     * @param request menu-item update request
     * @return the mapped updated menu item
     */
    @Transactional
    public MenuItemResponse updateMenuItem(Long menuItemId, MenuItemRequest request) {
        MenuItem existingItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId));
        menuItemMapper.updateEntityFromRequest(request, existingItem);
        MenuItem updatedItem = menuItemRepository.save(existingItem);

        return menuItemMapper.toResponse(updatedItem);
    }

    /**
     * Toggles an existing menu item's availability.
     *
     * @param menuItemId menu-item identifier
     * @return the mapped menu item
     */
    @Transactional
    public MenuItemResponse updateItemAvailability(Long menuItemId) {
        MenuItem existingItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId));
        existingItem.setIsAvailable(!existingItem.getIsAvailable());
        MenuItem flippedAvailability = menuItemRepository.save(existingItem);

        return menuItemMapper.toResponse(flippedAvailability);
    }

    /**
     * Soft-deletes an existing menu item.
     *
     * @param menuItemId menu-item identifier
     */
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

    /**
     * Updates a menu item only when it belongs to the authenticated restaurant.
     *
     * @param menuItemId menu-item identifier
     * @param restaurantId owning restaurant identifier
     * @param request mutable menu-item fields
     * @return updated menu item
     * @throws ResourceNotFoundException if the menu item does not exist
     * @throws ForbiddenException if ownership validation fails
     */
    @Transactional
    public MenuItemResponse updateMenuItemForRestaurant(
            Long menuItemId, Long restaurantId, MenuItemRequest request
    ) {
        MenuItem existingItem = findOwnedMenuItem(menuItemId, restaurantId);
        menuItemMapper.updateEntityFromRequest(request, existingItem);
        return menuItemMapper.toResponse(menuItemRepository.save(existingItem));
    }

    /**
     * Toggles availability only for a menu item owned by the authenticated restaurant.
     *
     * @param menuItemId menu-item identifier
     * @param restaurantId owning restaurant identifier
     * @return updated menu item
     * @throws ResourceNotFoundException if the menu item does not exist
     * @throws ForbiddenException if ownership validation fails
     */
    @Transactional
    public MenuItemResponse updateItemAvailabilityForRestaurant(
            Long menuItemId, Long restaurantId
    ) {
        MenuItem existingItem = findOwnedMenuItem(menuItemId, restaurantId);
        existingItem.setIsAvailable(!existingItem.getIsAvailable());
        return menuItemMapper.toResponse(menuItemRepository.save(existingItem));
    }

    /**
     * Soft-deletes a menu item only when it belongs to the authenticated restaurant.
     *
     * @param menuItemId menu-item identifier
     * @param restaurantId owning restaurant identifier
     * @throws ResourceNotFoundException if the menu item does not exist
     * @throws ResourceAlreadyTaggedAsDeletedException if the item is already deleted
     * @throws ForbiddenException if ownership validation fails
     */
    @Transactional
    public void deleteMenuItemSoftForRestaurant(Long menuItemId, Long restaurantId) {
        MenuItem item = findOwnedMenuItem(menuItemId, restaurantId);
        if (Boolean.TRUE.equals(item.getIsDeleted())) {
            throw new ResourceAlreadyTaggedAsDeletedException(menuItemId, "Menu item");
        }
        item.setIsDeleted(true);
        menuItemRepository.save(item);
    }

    /**
     * Finds a menu item and verifies it belongs to the requested restaurant.
     *
     * @param menuItemId menu-item identifier
     * @param restaurantId owning restaurant identifier
     * @return owned menu item
     * @throws ResourceNotFoundException if the menu item does not exist
     * @throws ForbiddenException if ownership validation fails
     */
    private MenuItem findOwnedMenuItem(Long menuItemId, Long restaurantId) {
        MenuItem item = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item", menuItemId));
        if (item.getRestaurant() == null
                || !restaurantId.equals(item.getRestaurant().getRestaurantId())) {
            throw new ForbiddenException("Menu item does not belong to the authenticated restaurant.");
        }
        return item;
    }
}
