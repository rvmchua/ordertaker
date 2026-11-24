package com.rvmc.ordertaker.controller;

import com.rvmc.ordertaker.dto.*;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.service.CartService;
import com.rvmc.ordertaker.service.CustomerService;
import com.rvmc.ordertaker.service.MenuService;
import com.rvmc.ordertaker.service.OrderService;
import com.rvmc.ordertaker.security.AuthenticatedUserService;
import com.rvmc.ordertaker.entity.Customer;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
    private final AuthenticatedUserService authenticatedUserService;

    // -------------------------------------------------------------------------
    // Profile
    // -------------------------------------------------------------------------

    /**
     * Returns the profile for the requested customer.
     *
     * @param authentication authenticated customer principal
     * @return mapped customer profile
     */
    @GetMapping("profile")
    public CustomerProfileResponse findProfile(Authentication authentication) {
        Customer customer = authenticatedUserService.getCustomer(authentication);
        return customerService.findByCustomerId(customer.getCustomerId());
    }

    /**
     * Applies a customer profile update.
     *
     * @param request profile update request
     * @param authentication authenticated customer principal
     * @return mapped updated profile
     */
    @PutMapping("profile")
    public CustomerProfileResponse patchProfile(
            @Valid @RequestBody CustomerProfileRequest request,
            Authentication authentication
    ) {
        Customer customer = authenticatedUserService.getCustomer(authentication);
        return customerService.editProfile(customer.getCustomerId(), request);
    }

    // -------------------------------------------------------------------------
    // Order
    // -------------------------------------------------------------------------

    /**
     * Lists orders owned by a customer.
     *
     * @param authentication authenticated customer principal
     * @return mapped customer orders
     */
    @GetMapping("order")
    public List<OrderResponse> getAllOrders(Authentication authentication) {
        Customer customer = authenticatedUserService.getCustomer(authentication);
        return orderService.getAllCustomerOrders(customer.getCustomerId());
    }

    /**
     * Returns one order owned by a customer.
     *
     * @param orderId order identifier
     * @param authentication authenticated customer principal
     * @return mapped order
     */
    @GetMapping("order/{orderId}")
    public OrderResponse getSingleOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        Customer customer = authenticatedUserService.getCustomer(authentication);
        return orderService.getCustomerOrderById(orderId, customer.getCustomerId());
    }

    /**
     * Checks out a customer's active cart.
     *
     * @param authentication authenticated customer principal
     * @return the created order
     */
    @PostMapping("order")
    public OrderResponse checkoutCartToCreateOrder(Authentication authentication) {
        Customer customer = authenticatedUserService.getCustomer(authentication);
        return orderService.createOrder(customer.getCustomerId());
    }

    /**
     * Cancels an eligible customer order.
     *
     * @param orderId order identifier
     * @param authentication authenticated customer principal
     * @param status cancellation status
     * @return the mapped cancelled order
     */
    @PatchMapping("order/{orderId}")
    public OrderResponse cancelOrder (
            @PathVariable Long orderId,
            @Valid @RequestBody @NotNull UpdateOrderStatus status,
            Authentication authentication
    ) {
        if (!status.status().equals(OrderStatus.CANCELLED)) {
                throw new IllegalArgumentException("Customers can only cancel their order.");
        }
        Customer customer = authenticatedUserService.getCustomer(authentication);
        return orderService.cancelOrderByCustomer(status, orderId, customer.getCustomerId());
    }

    // -------------------------------------------------------------------------
    // Cart
    // -------------------------------------------------------------------------
    /**
     * Returns a customer's active cart.
     *
     * @param authentication authenticated customer principal
     * @return mapped active cart
     */
    @GetMapping("cart")
    public CartResponse getActiveCart(Authentication authentication){
        Customer customer = authenticatedUserService.getCustomer(authentication);
        return cartService.getActiveCart(customer.getCustomerId());
    }

    /**
     * Creates a customer's active cart.
     *
     * @param request cart creation request
     * @param authentication authenticated customer principal
     * @return mapped created cart
     */
    @PostMapping("cart")
    public CartResponse createCart(
            @Valid @RequestBody @NotNull CreateCartRequest request,
            Authentication authentication
    ) {
        Customer customer = authenticatedUserService.getCustomer(authentication);
        CreateCartRequest ownedRequest = new CreateCartRequest(
                customer.getCustomerId(),
                request.restaurantId(),
                request.cartItems()
        );
        return cartService.createActiveCart(ownedRequest);
    }

    /**
     * Replaces or clears a customer's active cart items.
     *
     * @param request cart replacement request
     * @param authentication authenticated customer principal
     * @return mapped updated cart
     */
    @PutMapping("cart")
    public CartResponse updateCart(
            @Valid @RequestBody @NotNull UpdateCartRequest request,
            Authentication authentication
    ) {
        Customer customer = authenticatedUserService.getCustomer(authentication);
        UpdateCartRequest ownedRequest = new UpdateCartRequest(
                customer.getCustomerId(),
                request.restaurantId(),
                request.cartItems()
        );
        return cartService.updateActiveCart(ownedRequest);
    }

}
