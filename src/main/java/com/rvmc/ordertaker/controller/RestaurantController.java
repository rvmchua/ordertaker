package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.dto.UpdateOrderStatus;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import com.rvmc.ordertaker.service.RestaurantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

//    Orders

    /**
     * Lists orders belonging to a restaurant.
     *
     * @param restaurantId restaurant identifier
     * @return mapped restaurant orders
     */
    @GetMapping("order/{restaurantId}")
    public List<OrderResponse> getAllOrders(@PathVariable Long restaurantId) {
        return orderService.getAllRestaurantOrders(restaurantId);
    }

    /**
     * Applies an allowed status transition to a restaurant order.
     *
     * @param orderId order identifier
     * @param restaurantId restaurant identifier
     * @param status requested status
     * @return mapped updated order
     */
    @PatchMapping("order/{orderId}")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam @jakarta.validation.constraints.NotNull Long restaurantId,
            @Valid @RequestBody UpdateOrderStatus status
    ) {
        return orderService.updateOrderStatusByRestaurant(status, orderId, restaurantId);
    }

//    Restaurants

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


//    Menu
    /**
     * Creates a menu item.
     *
     * @param request menu-item creation request
     * @return mapped created menu item
     */
    @PostMapping("menu")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse createMenu(@Valid @RequestBody MenuItemRequest request) {
        return menuService.createMenuItem(request);
    }

    /**
     * Replaces mutable fields on a menu item.
     *
     * @param request menu-item update request
     * @param menuItemId menu-item identifier
     * @return mapped updated menu item
     */
    @PutMapping("menu/{menuItemId}")
    public MenuItemResponse updateMenuItem(@Valid @RequestBody MenuItemRequest request, @PathVariable Long menuItemId) {
        return menuService.updateMenuItem(menuItemId, request);
    }

    /**
     * Toggles a menu item's availability.
     *
     * @param menuItemId menu-item identifier
     * @return mapped menu item
     */
    @PatchMapping("menu/{menuItemId}")
    public MenuItemResponse toggleMenuItemAvailability(@PathVariable Long menuItemId) {
        return menuService.updateItemAvailability(menuItemId);
    }

    /**
     * Soft-deletes a menu item.
     *
     * @param menuItemId menu-item identifier
     * @return success response
     */
    @DeleteMapping("menu/{menuItemId}")
    public ResponseEntity<Map<String, String>> deleteMenuItem(@PathVariable Long menuItemId) {
        menuService.deleteMenuItemSoft(menuItemId);
        return ResponseEntity.ok(Map.of("message", " deleted successfully"));
    }

}
