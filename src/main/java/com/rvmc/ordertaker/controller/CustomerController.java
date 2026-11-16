package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.*;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.service.CartService;
import com.rvmc.ordertaker.service.CustomerService;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {
    private final CustomerService customerService;
    private final MenuService menuService;
    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping("profile")
    public CustomerProfileResponse findProfile(Long customerId) {
        return customerService.findByCustomerId(customerId);
    }

//    TODO: Check how to use PatchMapping instead of PutMapping
    @PatchMapping("profile")
    public CustomerProfileResponse patchProfile(@RequestBody CustomerProfileRequest request) {
        return customerService.editProfile(request);
    }

    /*
    * Orders
    // */

    @GetMapping("order")
    public List<OrderResponse> getAllOrders(@Valid @RequestBody Long customerId) {
        return orderService.getAllCustomerOrders(customerId);
    }

    @GetMapping("order/{orderId}")
    public OrderResponse getSingleOrder (@Valid @RequestBody Long customerId, @PathVariable Long orderId) {
        return orderService.getCustomerOrderById(orderId, customerId);
    }

    @PostMapping("order")
    public OrderResponse createOrder (@Valid @RequestBody OrderRequest request) {
        return orderService.createOrder(request);
    }

    @PatchMapping("order/{orderId}")
    public OrderResponse cancelOrder (@PathVariable Long orderId, @Valid @RequestBody UpdateOrderStatus status) {
        if (!status.status().equals(OrderStatus.CANCELLED)) {
            throw new RuntimeException("Customers can only cancel their order.");
        }
        return orderService.updateOrderStatus(status, orderId);
    }
}
