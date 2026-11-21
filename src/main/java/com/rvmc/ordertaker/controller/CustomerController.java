package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.*;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.service.CartService;
import com.rvmc.ordertaker.service.CustomerService;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
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

    // -------------------------------------------------------------------------
    // Profile
    // -------------------------------------------------------------------------

    @GetMapping("profile")
    public CustomerProfileResponse findProfile(Long customerId) {
        return customerService.findByCustomerId(customerId);
    }

    @PutMapping("profile")
    public CustomerProfileResponse patchProfile(@RequestBody CustomerProfileRequest request) {
        return customerService.editProfile(request);
    }

    // -------------------------------------------------------------------------
    // Order
    // -------------------------------------------------------------------------

    @GetMapping("order")
    public List<OrderResponse> getAllOrders(@Valid @RequestBody Long customerId) {
        return orderService.getAllCustomerOrders(customerId);
    }

    @GetMapping("order/{orderId}")
    public OrderResponse getSingleOrder (@Valid @RequestBody Long customerId, @PathVariable Long orderId) {
        return orderService.getCustomerOrderById(orderId, customerId);
    }

    @PostMapping("order")
    public OrderResponse checkoutCartToCreateOrder (@Valid @RequestBody Long customerId) {
        return orderService.createOrder(customerId);
    }

    @PatchMapping("order/{orderId}")
    public OrderResponse cancelOrder (@PathVariable Long orderId, @Valid @RequestBody @NotNull UpdateOrderStatus status) {
        if (!status.status().equals(OrderStatus.CANCELLED)) {
            throw new RuntimeException("Customers can only cancel their order.");
        }
        return orderService.updateOrderStatus(status, orderId);
    }

    // -------------------------------------------------------------------------
    // Cart
    // -------------------------------------------------------------------------
    @GetMapping("cart")
    public CartResponse getActiveCart(@Valid @RequestBody Long customerId){
        return cartService.getActiveCart(customerId);
    }

    @PostMapping("cart")
    public CartResponse createCart(@Valid @RequestBody @NotNull CreateCartRequest request) {
        return cartService.createActiveCart(request);
    }

    @PutMapping("cart")
    public CartResponse updateCart(@Valid @RequestBody @NotNull UpdateCartRequest request) {
        return cartService.updateActiveCart(request);
    }

}
