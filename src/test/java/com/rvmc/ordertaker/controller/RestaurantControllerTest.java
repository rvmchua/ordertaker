package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.RestaurantProfileResponse;
import com.rvmc.ordertaker.exception.GlobalExceptionHandler;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import com.rvmc.ordertaker.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantController.class)
@Import(GlobalExceptionHandler.class)
class RestaurantControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private RestaurantService restaurantService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private MenuService menuService;

    @Test
    void getRestaurants_shouldReturnAllWhenQueryIsAbsent() throws Exception {
        RestaurantProfileResponse response = new RestaurantProfileResponse(
                1L, "Bistro", "Address 1", "Address 2", Collections.emptyList()
        );
        when(restaurantService.getAllRestaurants()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/restaurants"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].restaurantId").value(1))
                .andExpect(jsonPath("$[0].restaurantName").value("Bistro"));
        verify(restaurantService).getAllRestaurants();
    }

    @Test
    void getRestaurants_shouldUseQueryWhenProvided() throws Exception {
        when(restaurantService.getRestaurantByQuery("Pizza")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/restaurants").param("restaurant", "Pizza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
        verify(restaurantService).getRestaurantByQuery("Pizza");
    }

    @Test
    void getRestaurantById_shouldReturnRestaurant() throws Exception {
        RestaurantProfileResponse response = new RestaurantProfileResponse(
                5L, "Pizza Place", "Address 1", "Address 2", Collections.emptyList()
        );
        when(restaurantService.getRestaurantById(5L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/restaurants/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.restaurantId").value(5));
    }

    @Test
    void getRestaurantById_shouldReturnNotFound_whenRestaurantDoesNotExist() throws Exception {
        when(restaurantService.getRestaurantById(99L))
                .thenThrow(new ResourceNotFoundException("Restaurant", 99L));

        mockMvc.perform(get("/api/v1/restaurants/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void createMenu_shouldReturnBadRequest_whenRequiredFieldsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/v1/restaurants/menu")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"restaurantId\":5,\"itemName\":\" \",\"price\":0.50}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.itemName").exists())
                .andExpect(jsonPath("$.price").exists());
    }
}
