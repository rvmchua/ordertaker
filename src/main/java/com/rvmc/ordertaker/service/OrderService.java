package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.OrderItemRequest;
import com.rvmc.ordertaker.dto.OrderRequest;
import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.UpdateOrderStatus;
import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Order;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.OrderMapper;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.MenuItemRepository;
import com.rvmc.ordertaker.repository.OrderRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Getter
@Setter
public class OrderService {
    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final OrderMapper orderMapper;
    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;

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
                .orElseThrow(() -> new ResourceNotFoundException("Customer", restaurantId));
        return orderRepository
                .findAllByRestaurant(restaurant)
                .stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {

        List<Long> menuItemIdList = request
                .orderItems()
                .stream()
                .map(OrderItemRequest::menuItemId)
                .toList();

        List<MenuItem> menuItems = menuItemRepository
                .findAllByIdAndIsDeleted(menuItemIdList, false);

        validateOrderItems(menuItems, menuItemIdList, request);

        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", request.restaurantId()));
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));

        Order order = Order.create(restaurant, customer, menuItems, request);

        return orderMapper.toResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderResponse updateOrderStatus(UpdateOrderStatus status, Long orderId) {
        if (!orderRepository.existsById(orderId)) {
            throw new ResourceNotFoundException("Order", orderId);
        }

        Order existingOrder = orderRepository.findByOrderIdAndStatusNotIn(orderId, List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED))
                .orElseThrow(() -> new ResourceNotFoundException("Cannot update order that was already cancelled or received."));

        // Validate the transition
        if (!existingOrder.getStatus().canTransitionTo(status.status())) {
            throw new IllegalStateException("Invalid status transition from " + existingOrder.getStatus() + " to " + status.status());
        }

        existingOrder.setStatus(status.status());
        Order savedOrder = orderRepository.save(existingOrder);

        return orderMapper.toResponse(savedOrder);
    }

    /*
     * Validation Helper Functions
     **/

    private void validateOrderItems(List<MenuItem> menuItemList, List<Long> menuItemIds, OrderRequest request)  {
        Long restaurantId = request.restaurantId();

//        Check if there are missing IDs not found
        if (menuItemList.size() != request.orderItems().size()) {
            Set<Long> foundIds = menuItemList.stream()
                    .map(MenuItem::getMenuItemId).collect(Collectors.toSet());

            List<Long> missingIds = menuItemIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();

            throw new ResourceNotFoundException("Menu items not found for IDs: " + missingIds);
        }

//        Validate if restaurant has the item
        boolean invalidRestaurantItems = menuItemList.stream()
                .anyMatch(item -> !item.getRestaurant().getRestaurantId().equals(restaurantId));

        if (invalidRestaurantItems) {
            throw new IllegalArgumentException("One or more menu items do not belong to this restaurant.");
        }

    }

}
