package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.CustomerProfileRequest;
import com.rvmc.ordertaker.dto.CustomerProfileResponse;
import com.rvmc.ordertaker.service.CustomerService;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final MenuService menuService;
    private final OrderService orderService;

    @GetMapping("profile")
    public CustomerProfileResponse findProfile(Long customerId) {
        return customerService.findByCustomerId(customerId);
    }

//    TODO: Check how to use PatchMapping instead of PutMapping
    @PatchMapping("profile")
    public CustomerProfileResponse patchProfile(
            @RequestBody CustomerProfileRequest request
    ) {
        return customerService.editProfile(request);
    }
}
