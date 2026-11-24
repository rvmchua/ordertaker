package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.service.RestaurantService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {
    private final RestaurantService restaurantService;

    @DeleteMapping("restaurants/{restaurantId}")
    public ResponseEntity<Map<String, String>> deleteRestaurant(
            @PathVariable Long restaurantId
    ) {
        restaurantService.hardDeleteRestaurantByAdmin(restaurantId);
        return ResponseEntity.ok(Map.of("message", "Restaurant permanently deleted"));
    }

    @PatchMapping("restaurants/{restaurantId}/reactivate")
    public ResponseEntity<Map<String, String>> reactivateRestaurant(
            @PathVariable Long restaurantId
    ) {
        restaurantService.reactivateRestaurantByAdmin(restaurantId);
        return ResponseEntity.ok(Map.of("message", "Restaurant reactivated"));
    }
}
