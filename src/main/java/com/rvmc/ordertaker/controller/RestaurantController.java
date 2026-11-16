package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import com.rvmc.ordertaker.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/restaurants")
@RequiredArgsConstructor
public class RestaurantController {
    public final RestaurantService restaurantService;
    public final OrderService orderService;
    public final MenuService menuService;

    @GetMapping("{restaurantId}")
    public List<OrderResponse> getAllOrders(@PathVariable Long restaurantId) {
        return orderService.getAllRestaurantOrders(restaurantId);
    }

}
