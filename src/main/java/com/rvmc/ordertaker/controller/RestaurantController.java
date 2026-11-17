package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.MenuItemRequest;
import com.rvmc.ordertaker.dto.MenuItemResponse;
import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
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

    @GetMapping("order/{restaurantId}")
    public List<OrderResponse> getAllOrders(@PathVariable Long restaurantId) {
        return orderService.getAllRestaurantOrders(restaurantId);
    }

//    Restaurants

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

    @GetMapping("{restaurantId}")
    public RestaurantProfileResponse getRestaurantById(@PathVariable Long restaurantId) {
        return restaurantService.getRestaurantById(restaurantId);
    }


//    Menu
    @PostMapping("menu")
    @ResponseStatus(HttpStatus.CREATED)
    public MenuItemResponse createMenu(@Valid @RequestBody MenuItemRequest request) {
        return menuService.createMenuItem(request);
    }

    @PutMapping("menu/{menuItemId}")
    public MenuItemResponse updateMenuItem(@Valid @RequestBody MenuItemRequest request, @PathVariable Long menuItemId) {
        return menuService.updateMenuItem(menuItemId, request);
    }

    @PatchMapping("menu/{menuItemId}")
    public MenuItemResponse toggleMenuItemAvailability(@PathVariable Long menuItemId) {
        return menuService.updateItemAvailability(menuItemId);
    }

    @DeleteMapping("menu/{menuItemId}")
    public ResponseEntity<Map<String, String>> deleteMenuItem(@PathVariable Long menuItemId) {
        menuService.deleteMenuItemSoft(menuItemId);
        return ResponseEntity.ok(Map.of("message", " deleted successfully"));
    }

}
