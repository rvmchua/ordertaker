package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.dto.RestaurantProfileRequest;
import com.rvmc.ordertaker.dto.UpdateOrderStatus;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import com.rvmc.ordertaker.service.RestaurantService;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.security.AuthenticatedUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    public final RestaurantService restaurantService;
    public final OrderService orderService;
    public final MenuService menuService;
    private final AuthenticatedUserService authenticatedUserService;

//    Orders

    /**
     * Lists orders belonging to a restaurant.
     *
     * @param restaurantId restaurant identifier
     * @param authentication authenticated restaurant owner principal
     * @return mapped restaurant orders
     */
    @GetMapping("order/{restaurantId}")
    public List<OrderResponse> getAllOrders(
            @PathVariable Long restaurantId,
            Authentication authentication
    ) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        return orderService.getAllRestaurantOrders(restaurant.getRestaurantId());
    }

    /**
     * Applies an allowed status transition to a restaurant order.
     *
     * @param orderId order identifier
     * @param restaurantId restaurant identifier
     * @param status requested status
     * @param authentication authenticated restaurant owner principal
     * @return mapped updated order
     */
    @PatchMapping("order/{orderId}")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam @jakarta.validation.constraints.NotNull Long restaurantId,
            @Valid @RequestBody UpdateOrderStatus status,
            Authentication authentication
    ) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        return orderService.updateOrderStatusByRestaurant(
                status, orderId, restaurant.getRestaurantId()
        );
    }

//    Restaurants

    /**
     * Returns the authenticated owner's restaurant profile.
     */
    @GetMapping("profile")
    public RestaurantProfileResponse getProfile(Authentication authentication) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        return restaurantService.findProfile(restaurant.getRestaurantId());
    }

    /**
     * Applies a partial update to the authenticated owner's restaurant profile.
     */
    @PutMapping("profile")
    public RestaurantProfileResponse updateProfile(
            @Valid @RequestBody RestaurantProfileRequest request,
            Authentication authentication
    ) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        return restaurantService.editProfile(restaurant.getRestaurantId(), request);
    }

    /**
     * Soft-deletes the authenticated restaurant owner's profile.
     */
    @DeleteMapping("profile")
    public ResponseEntity<Map<String, String>> deleteProfile(Authentication authentication) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        restaurantService.deleteProfile(restaurant.getRestaurantId());
        return ResponseEntity.ok(Map.of("message", "Restaurant deleted successfully"));
    }

    /**
     * Lists restaurants or searches by name.
     *
     * @param restaurant optional name-search fragment
     * @return matching restaurant profiles
     */
    @GetMapping
    public List<RestaurantProfileResponse> getRestaurants(
            @RequestParam(required = false) String restaurant
    ) {
        if (restaurant == null) {
        return restaurantService.getAllRestaurants();
        } else {
            return restaurantService.getRestaurantByQuery(restaurant);
        }
    }

    /**
     * Finds a restaurant profile by identifier.
     *
     * @param restaurantId restaurant identifier
     * @return mapped restaurant profile
     */
    @GetMapping("{restaurantId}")
    public RestaurantProfileResponse getRestaurantById(@PathVariable Long restaurantId) {
        return restaurantService.getRestaurantById(restaurantId);
    }

    /**
     * Lists active, non-deleted menu items for a restaurant.
     *
     * @param restaurantId restaurant identifier
     * @return active menu items
     */
    @GetMapping("{restaurantId}/menu")
    public List<MenuItemResponse> getMenu(@PathVariable Long restaurantId) {
        return menuService.getMenuFromRestaurant(restaurantId);
    }

    /**
     * Finds one active, non-deleted menu item.
     *
     * @param menuItemId menu-item identifier
     * @return mapped menu item
     */
    @GetMapping("menu/{menuItemId}")
    public MenuItemResponse getMenuItem(@PathVariable Long menuItemId) {
        return menuService.getMenuItemById(menuItemId);
    }


//    Menu
    /**
     * Creates a menu item.
     *
     * @param request menu-item creation request
     * @param authentication authenticated restaurant owner principal
     * @return mapped created menu item
     */
    @PostMapping("menu")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse createMenu(
            @Valid @RequestBody MenuItemRequest request,
            Authentication authentication
    ) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        MenuItemRequest ownedRequest = new MenuItemRequest(
                restaurant.getRestaurantId(),
                request.itemName(),
                request.description(),
                request.price(),
                request.isAvailable(),
                request.isDeleted()
        );
        return menuService.createMenuItem(ownedRequest);
    }

    /**
     * Replaces mutable fields on a menu item.
     *
     * @param request menu-item update request
     * @param menuItemId menu-item identifier
     * @param authentication authenticated restaurant owner principal
     * @return mapped updated menu item
     */
    @PutMapping("menu/{menuItemId}")
    public MenuItemResponse updateMenuItem(
            @Valid @RequestBody MenuItemRequest request,
            @PathVariable Long menuItemId,
            Authentication authentication
    ) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        return menuService.updateMenuItemForRestaurant(
                menuItemId, restaurant.getRestaurantId(), request
        );
    }

    /**
     * Toggles a menu item's availability.
     *
     * @param menuItemId menu-item identifier
     * @param authentication authenticated restaurant owner principal
     * @return mapped menu item
     */
    @PatchMapping("menu/{menuItemId}")
    public MenuItemResponse toggleMenuItemAvailability(
            @PathVariable Long menuItemId,
            Authentication authentication
    ) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        return menuService.updateItemAvailabilityForRestaurant(
                menuItemId, restaurant.getRestaurantId()
        );
    }

    /**
     * Soft-deletes a menu item.
     *
     * @param menuItemId menu-item identifier
     * @param authentication authenticated restaurant owner principal
     * @return success response
     */
    @DeleteMapping("menu/{menuItemId}")
    public ResponseEntity<Map<String, String>> deleteMenuItem(
            @PathVariable Long menuItemId,
            Authentication authentication
    ) {
        Restaurant restaurant = authenticatedUserService.getRestaurant(authentication);
        menuService.deleteMenuItemSoftForRestaurant(
                menuItemId, restaurant.getRestaurantId()
        );
        return ResponseEntity.ok(Map.of("message", " deleted successfully"));
    }

}
