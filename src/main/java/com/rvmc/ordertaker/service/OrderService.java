package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.UpdateOrderStatus;
import com.rvmc.ordertaker.entity.Cart;
import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.Order;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.exception.OrderTaggedAsReceivedOrCancelledException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.OrderMapper;
import com.rvmc.ordertaker.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final CartRepository cartRepository;
    private final CartService cartService;

    public OrderResponse getCustomerOrderById(Long orderId, Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        Order order = orderRepository.findByOrderIdAndCustomer(orderId, customer)
                .orElseThrow(() -> new ResourceNotFoundException("Order ID", orderId));

        return orderMapper.toResponse(order);
    }

    public OrderResponse getRestaurantOrderById(Long orderId, Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        Order order = orderRepository.findByOrderIdAndRestaurant(orderId, restaurant)
                .orElseThrow(() -> new ResourceNotFoundException("Order ID", orderId));

        return orderMapper.toResponse(order);
    }

    public List<OrderResponse> getAllCustomerOrders(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        return orderRepository
                .findAllByCustomer(customer)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    public List<OrderResponse> getAllRestaurantOrders(Long restaurantId) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", restaurantId));
        return orderRepository
                .findAllByRestaurant(restaurant)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse createOrder(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        Cart activeCart = cartRepository.findByCustomerAndIsActive(customer, true)
                        .orElseThrow(() -> new ResourceNotFoundException("No active cart found."));
        if (activeCart.getCartItems() == null) {
            throw new ResourceNotFoundException("Cart cannot be empty.");
        }
        Order order = Order.create(activeCart);
        try {
            Order savedOrder = orderRepository.save(order);
            OrderResponse response = orderMapper.toResponse(savedOrder);
            Cart deactivatedCart = cartService.deactivateCart(activeCart);
            return response;
        } catch (DataIntegrityViolationException ex) {
            // e.g., unique constraint, foreign key constraint, or non-null column violation
            throw new IllegalStateException("Order could not be saved due to a data constraint violation.", ex);
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Concurrent modification conflict (e.g., 409 Conflict)
            throw new IllegalStateException("Order state changed concurrently while saving.", ex);
        } catch (DataAccessException ex) {
            // Broader database/Spring Data failure
            throw new IllegalStateException("Failed to save order due to a persistence error.", ex);
        }
    }

    @Transactional
    public OrderResponse updateOrderStatus(UpdateOrderStatus status, Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        Order existingOrder = orderRepository.findByOrderIdAndStatusNotIn(orderId, List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED))
                .orElseThrow(() -> new OrderTaggedAsReceivedOrCancelledException("Cannot update order that was already cancelled or received."));

        // Validate the transition
        if (!existingOrder.getStatus().canTransitionTo(status.status())) {
            throw new IllegalStateException("Invalid status transition from " + existingOrder.getStatus() + " to " + status.status());
        }

        existingOrder.setStatus(status.status());
        Order savedOrder = orderRepository.save(existingOrder);

        return orderMapper.toResponse(savedOrder);
    }

}
