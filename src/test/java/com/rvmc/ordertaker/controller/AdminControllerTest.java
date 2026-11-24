package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.exception.GlobalExceptionHandler;
import com.rvmc.ordertaker.repository.UserRepository;
import com.rvmc.ordertaker.security.JwtService;
import com.rvmc.ordertaker.service.RestaurantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
@Import(GlobalExceptionHandler.class)
class AdminControllerTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private RestaurantService restaurantService;
    @MockitoBean private JwtService jwtService;
    @MockitoBean private UserRepository userRepository;

    @Test
    void deleteRestaurant_shouldInvokeAdminDeletion() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/restaurants/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restaurant permanently deleted"));
        verify(restaurantService).hardDeleteRestaurantByAdmin(5L);
    }

    @Test
    void reactivateRestaurant_shouldInvokeAdminReactivation() throws Exception {
        mockMvc.perform(patch("/api/v1/admin/restaurants/5/reactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Restaurant reactivated"));
        verify(restaurantService).reactivateRestaurantByAdmin(5L);
    }
}
