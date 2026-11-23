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

    /**
     * Returns the profile for the requested customer.
     *
     * @param customerId customer identifier
     * @return mapped customer profile
     */
    @GetMapping("profile")
    public CustomerProfileResponse findProfile(Long customerId) {
        return customerService.findByCustomerId(customerId);
    }

    /**
     * Applies a customer profile update.
     *
     * @param request profile update request
     * @return mapped updated profile
     */
    @PutMapping("profile")
    public CustomerProfileResponse patchProfile(@RequestBody CustomerProfileRequest request) {
        return customerService.editProfile(request);
    }

    // -------------------------------------------------------------------------
    // Order
    // -------------------------------------------------------------------------

    /**
     * Lists orders owned by a customer.
     *
     * @param customerId customer identifier
     * @return mapped customer orders
     */
    @GetMapping("order")
    public List<OrderResponse> getAllOrders(@Valid @RequestBody Long customerId) {
        return orderService.getAllCustomerOrders(customerId);
    }

    /**
     * Returns one order owned by a customer.
     *
     * @param customerId customer identifier
     * @param orderId order identifier
     * @return mapped order
     */
    @GetMapping("order/{orderId}")
    public OrderResponse getSingleOrder (@Valid @RequestBody Long customerId, @PathVariable Long orderId) {
        return orderService.getCustomerOrderById(orderId, customerId);
    }

    /**
     * Checks out a customer's active cart.
     *
     * @param customerId customer identifier
     * @return the created order
     */
    @PostMapping("order")
    public OrderResponse checkoutCartToCreateOrder (@Valid @RequestBody Long customerId) {
        return orderService.createOrder(customerId);
    }

    /**
     * Cancels an eligible customer order.
     *
     * @param orderId order identifier
     * @param customerId customer identifier
     * @param status cancellation status
     * @return the mapped cancelled order
     */
    @PatchMapping("order/{orderId}")
    public OrderResponse cancelOrder (
            @PathVariable Long orderId,
            @RequestParam @NotNull Long customerId,
            @Valid @RequestBody @NotNull UpdateOrderStatus status
    ) {
        if (!status.status().equals(OrderStatus.CANCELLED)) {
                throw new IllegalArgumentException("Customers can only cancel their order.");
        }
        return orderService.cancelOrderByCustomer(status, orderId, customerId);
    }

    // -------------------------------------------------------------------------
    // Cart
    // -------------------------------------------------------------------------
    /**
     * Returns a customer's active cart.
     *
     * @param customerId customer identifier
     * @return mapped active cart
     */
    @GetMapping("cart")
    public CartResponse getActiveCart(@Valid @RequestBody Long customerId){
        return cartService.getActiveCart(customerId);
    }

    /**
     * Creates a customer's active cart.
     *
     * @param request cart creation request
     * @return mapped created cart
     */
    @PostMapping("cart")
    public CartResponse createCart(@Valid @RequestBody @NotNull CreateCartRequest request) {
        return cartService.createActiveCart(request);
    }

    /**
     * Replaces or clears a customer's active cart items.
     *
     * @param request cart replacement request
     * @return mapped updated cart
     */
    @PutMapping("cart")
    public CartResponse updateCart(@Valid @RequestBody @NotNull UpdateCartRequest request) {
        return cartService.updateActiveCart(request);
    }

}
