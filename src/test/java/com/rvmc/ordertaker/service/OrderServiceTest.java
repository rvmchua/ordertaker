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
import com.rvmc.ordertaker.exception.OrderTaggedAsReceivedOrCancelledException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.OrderMapper;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.MenuItemRepository;
import com.rvmc.ordertaker.repository.OrderRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private CustomerRepository customerRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderRepository orderRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @InjectMocks private OrderService orderService;

    /*
    * createOrder Tests
    */
    @Test void createOrder_shouldReturnMappedOrder_whenRequestIsValid() {
//         Arrange
        Long customerId = 1L;
        Long restaurantId = 5L;
        List<Long> itemIds = List.of(10L, 11L);

        OrderRequest request = new OrderRequest(
                customerId,
                restaurantId,
                List.of(
                        new OrderItemRequest(10L, (short) 2),
                        new OrderItemRequest(11L, (short) 1)
                )
        );

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .restaurantName("Burger Joint")
                .build();

        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .itemName("Cheeseburger")
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        MenuItem item11 = MenuItem.builder()
                .menuItemId(11L)
                .restaurant(restaurant)
                .itemName("Fries")
                .price(BigDecimal.valueOf(120.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(itemIds, false))
                .thenReturn(List.of(item10, item11));
        when(restaurantRepository.findById(restaurantId))
                .thenReturn(Optional.of(restaurant));
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);

        // Verify
        verify(menuItemRepository, times(1)).findAllByMenuItemIdInAndIsDeleted(itemIds, false);
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(customerRepository, times(1)).findById(customerId);
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(orderMapper, times(1)).toResponse(any(Order.class));
    }

    @Test void createOrder_shouldThrowResourceNotFoundException_whenRestaurantDoesNotExist() {
        // Arrange
        Long customerId = 1L;
        Long restaurantId = 99L;
        List<Long> itemIds = List.of(10L);

        OrderRequest request = new OrderRequest(
                customerId,
                restaurantId,
                List.of(new OrderItemRequest(10L, (short) 1))
        );

        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(Restaurant.builder().restaurantId(restaurantId).build())
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(itemIds, false))
                .thenReturn(List.of(item10));
        when(restaurantRepository.findById(restaurantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(customerRepository, never()).findById(any());
        verify(orderRepository, never()).save(any());
        verify(orderMapper, never()).toResponse(any());
    }

    @Test void createOrder_shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        // Arrange
        Long customerId = 99L;
        Long restaurantId = 5L;
        List<Long> itemIds = List.of(10L);

        OrderRequest request = new OrderRequest(
                customerId,
                restaurantId,
                List.of(new OrderItemRequest(10L, (short) 1))
        );

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();

        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(itemIds, false))
                .thenReturn(List.of(item10));
        when(restaurantRepository.findById(restaurantId))
                .thenReturn(Optional.of(restaurant));
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(customerRepository, times(1)).findById(customerId);
        verify(orderRepository, never()).save(any());
        verify(orderMapper, never()).toResponse(any());
    }

    @Test void createOrder_shouldThrowResourceNotFoundException_whenAnyMenuItemIsMissingOrDeleted() {
        // Arrange
        Long customerId = 1L;
        Long restaurantId = 5L;
        List<Long> requestedItemIds = List.of(10L, 11L);

        OrderRequest request = new OrderRequest(
                customerId,
                restaurantId,
                List.of(
                        new OrderItemRequest(10L, (short) 2),
                        new OrderItemRequest(11L, (short) 1)
                )
        );

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();

        // Only item 10L is returned; 11L is missing or soft-deleted
        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(requestedItemIds, false))
                .thenReturn(List.of(item10));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(request)
        );

        assertTrue(exception.getMessage().contains("[11]"));

        // Verify short-circuit before restaurant/customer/order steps
        verify(menuItemRepository, times(1)).findAllByMenuItemIdInAndIsDeleted(requestedItemIds, false);
        verifyNoInteractions(restaurantRepository, customerRepository, orderRepository, orderMapper);
    }

    @Test void createOrder_shouldThrowIllegalArgumentException_whenMenuItemBelongsToDifferentRestaurant() {
        // Arrange
        Long customerId = 1L;
        Long requestedRestaurantId = 5L;
        Long otherRestaurantId = 99L;
        List<Long> requestedItemIds = List.of(10L);

        OrderRequest request = new OrderRequest(
                customerId,
                requestedRestaurantId,
                List.of(new OrderItemRequest(10L, (short) 1))
        );

        Restaurant otherRestaurant = Restaurant.builder()
                .restaurantId(otherRestaurantId)
                .build();

        MenuItem itemFromWrongRestaurant = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(otherRestaurant)
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(requestedItemIds, false))
                .thenReturn(List.of(itemFromWrongRestaurant));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(request)
        );

        assertEquals(
                "One or more menu items do not belong to this restaurant.",
                exception.getMessage()
        );

        // Verify short-circuit before restaurant/customer/order steps
        verify(menuItemRepository, times(1)).findAllByMenuItemIdInAndIsDeleted(requestedItemIds, false);
        verifyNoInteractions(restaurantRepository, customerRepository, orderRepository, orderMapper);
    }

    @Test void createOrder_shouldDeduplicateItemIdsAndSucceed_whenRequestListsSameItemTwice() {
        // Arrange
        Long customerId = 1L;
        Long restaurantId = 5L;

        OrderRequest request = new OrderRequest(
                customerId,
                restaurantId,
                List.of(
                        new OrderItemRequest(10L, (short) 1),
                        new OrderItemRequest(10L, (short) 2)
                )
        );

        List<Long> expectedDistinctIds = List.of(10L);

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();
        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();
        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(expectedDistinctIds, false))
                .thenReturn(List.of(item10));
        when(restaurantRepository.findById(restaurantId))
                .thenReturn(Optional.of(restaurant));
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customer));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class)))
                .thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.createOrder(request);

        // Assert & Verify
        assertEquals(expectedResponse, result);
        verify(menuItemRepository, times(1))
                .findAllByMenuItemIdInAndIsDeleted(expectedDistinctIds, false);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /*
    * updateOrder Tests
    * */

    @Test void updateOrderStatus_shouldUpdateStatusSaveAndReturnResponse_whenTransitionIsValid() {
        // Arrange
        Long orderId = 100L;
        OrderStatus currentStatus = OrderStatus.PENDING;
        OrderStatus nextStatus = OrderStatus.CONFIRMED;
        UpdateOrderStatus request = new UpdateOrderStatus(nextStatus);

        Order existingOrder = Order.builder()
                .orderId(orderId)
                .status(currentStatus)
                .build();

        Order savedOrder = Order.builder()
                .orderId(orderId)
                .status(nextStatus)
                .build();

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(orderRepository.findByOrderIdAndStatusNotIn(
                orderId,
                List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED)
        )).thenReturn(Optional.of(existingOrder));
        when(orderRepository.save(existingOrder)).thenReturn(savedOrder);
        when(orderMapper.toResponse(savedOrder)).thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.updateOrderStatus(request, orderId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);
        assertEquals(nextStatus, existingOrder.getStatus());

        // Verify
        verify(orderRepository, times(1)).existsById(orderId);
        verify(orderRepository, times(1)).findByOrderIdAndStatusNotIn(
                orderId,
                List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED)
        );
        verify(orderRepository, times(1)).save(existingOrder);
        verify(orderMapper, times(1)).toResponse(savedOrder);
    }

    @Test void updateOrderStatus_shouldThrowResourceNotFoundException_whenOrderDoesNotExist() {
        // Arrange
        Long orderId = 999L;
        UpdateOrderStatus request = new UpdateOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.existsById(orderId)).thenReturn(false);

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.updateOrderStatus(request, orderId)
        );

        // Verify
        verify(orderRepository, times(1)).existsById(orderId);
        verify(orderRepository, never()).findByOrderIdAndStatusNotIn(any(), any());
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    @Test void updateOrderStatus_shouldThrowOrderTaggedAsReceivedOrCancelledException_whenOrderIsCancelledOrReceived() {
        // Arrange
        Long orderId = 100L;
        UpdateOrderStatus request = new UpdateOrderStatus(OrderStatus.CONFIRMED);

        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(orderRepository.findByOrderIdAndStatusNotIn(
                orderId,
                List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED)
        )).thenReturn(Optional.empty());

        // Act & Assert
        OrderTaggedAsReceivedOrCancelledException exception = assertThrows(
                OrderTaggedAsReceivedOrCancelledException.class,
                () -> orderService.updateOrderStatus(request, orderId)
        );

        assertEquals(
                "Cannot update order that was already cancelled or received.",
                exception.getMessage()
        );

        // Verify
        verify(orderRepository, times(1)).existsById(orderId);
        verify(orderRepository, times(1)).findByOrderIdAndStatusNotIn(
                orderId,
                List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED)
        );
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    @Test void updateOrderStatus_shouldThrowIllegalStateException_whenStatusTransitionIsInvalid() {
        // Arrange
        Long orderId = 100L;
        OrderStatus currentStatus = OrderStatus.PENDING;
        OrderStatus invalidNextStatus = OrderStatus.FOR_PICKUP; // skips intermediate states
        UpdateOrderStatus request = new UpdateOrderStatus(invalidNextStatus);

        Order existingOrder = Order.builder()
                .orderId(orderId)
                .status(currentStatus)
                .build();

        when(orderRepository.existsById(orderId)).thenReturn(true);
        when(orderRepository.findByOrderIdAndStatusNotIn(
                orderId,
                List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED)
        )).thenReturn(Optional.of(existingOrder));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.updateOrderStatus(request, orderId)
        );

        assertEquals(
                "Invalid status transition from " + currentStatus + " to " + invalidNextStatus,
                exception.getMessage()
        );
        assertEquals(currentStatus, existingOrder.getStatus());

        // Verify
        verify(orderRepository, times(1)).existsById(orderId);
        verify(orderRepository, times(1)).findByOrderIdAndStatusNotIn(
                orderId,
                List.of(OrderStatus.CANCELLED, OrderStatus.RECEIVED)
        );
        verify(orderRepository, never()).save(any());
        verifyNoInteractions(orderMapper);
    }

    /*
    * getCustomerOrderById Tests
    * */

    @Test void getCustomerOrderById_shouldReturnMappedResponse_whenCustomerAndOrderExistForCustomer() {
        // Arrange
        Long customerId = 1L;
        Long orderId = 100L;

        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        Order order = Order.builder()
                .orderId(orderId)
                .customer(customer)
                .build();

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.findByOrderIdAndCustomer(orderId, customer))
                .thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.getCustomerOrderById(orderId, customerId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(orderRepository, times(1)).findByOrderIdAndCustomer(orderId, customer);
        verify(orderMapper, times(1)).toResponse(order);
    }

    @Test void getCustomerOrderById_shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        // Arrange
        Long customerId = 99L;
        Long orderId = 100L;

        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getCustomerOrderById(orderId, customerId)
        );

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test void getCustomerOrderById_shouldThrowResourceNotFoundException_whenOrderDoesNotExistOrBelongsToAnotherCustomer() {
        // Arrange
        Long customerId = 1L;
        Long orderId = 100L;

        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.findByOrderIdAndCustomer(orderId, customer))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getCustomerOrderById(orderId, customerId)
        );

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(orderRepository, times(1)).findByOrderIdAndCustomer(orderId, customer);
        verifyNoInteractions(orderMapper);
    }

    /*
    * getRestaurantOrderById Tests
    * */

    @Test void getRestaurantOrderById_shouldReturnMappedResponse_whenRestaurantAndOrderExistForRestaurant() {
        // Arrange
        Long restaurantId = 5L;
        Long orderId = 100L;

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();

        Order order = Order.builder()
                .orderId(orderId)
                .restaurant(restaurant)
                .build();

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(orderRepository.findByOrderIdAndRestaurant(orderId, restaurant))
                .thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(expectedResponse);

        // Act
        OrderResponse result = orderService.getRestaurantOrderById(orderId, restaurantId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(orderRepository, times(1)).findByOrderIdAndRestaurant(orderId, restaurant);
        verify(orderMapper, times(1)).toResponse(order);
    }

    @Test void getRestaurantOrderById_shouldThrowResourceNotFoundException_whenRestaurantDoesNotExist() {
        // Arrange
        Long restaurantId = 99L;
        Long orderId = 100L;

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getRestaurantOrderById(orderId, restaurantId)
        );

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verifyNoInteractions(orderRepository, orderMapper);
    }

    @Test void getRestaurantOrderById_shouldThrowResourceNotFoundException_whenOrderDoesNotExistOrBelongsToAnotherRestaurant() {
        // Arrange
        Long restaurantId = 5L;
        Long orderId = 100L;

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(orderRepository.findByOrderIdAndRestaurant(orderId, restaurant))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getRestaurantOrderById(orderId, restaurantId)
        );

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(orderRepository, times(1)).findByOrderIdAndRestaurant(orderId, restaurant);
        verifyNoInteractions(orderMapper);
    }

    /*
    * getAllCustomerOrders Tests
    * */

    @Test void getAllCustomerOrders_shouldReturnMappedOrders_whenCustomerExistsAndHasOrders() {
        // Arrange
        Long customerId = 1L;
        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        Order order1 = Order.builder()
                .orderId(101L)
                .customer(customer)
                .build();

        Order order2 = Order.builder()
                .orderId(102L)
                .customer(customer)
                .build();

        OrderResponse response1 = mock(OrderResponse.class);
        OrderResponse response2 = mock(OrderResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.findAllByCustomer(customer)).thenReturn(List.of(order1, order2));
        when(orderMapper.toResponse(order1)).thenReturn(response1);
        when(orderMapper.toResponse(order2)).thenReturn(response2);

        // Act
        List<OrderResponse> result = orderService.getAllCustomerOrders(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(List.of(response1, response2), result);

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(orderRepository, times(1)).findAllByCustomer(customer);
        verify(orderMapper, times(1)).toResponse(order1);
        verify(orderMapper, times(1)).toResponse(order2);
    }

    @Test void getAllCustomerOrders_shouldReturnEmptyList_whenCustomerExistsWithNoOrders() {
        // Arrange
        Long customerId = 1L;
        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(orderRepository.findAllByCustomer(customer)).thenReturn(List.of());

        // Act
        List<OrderResponse> result = orderService.getAllCustomerOrders(customerId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(orderRepository, times(1)).findAllByCustomer(customer);
        verify(orderMapper, never()).toResponse(any());
    }

    @Test void getAllCustomerOrders_shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        // Arrange
        Long customerId = 99L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getAllCustomerOrders(customerId)
        );

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verifyNoInteractions(orderRepository, orderMapper);
    }

    /*
    * getAllRestaurantOrders Tests
    * */

    @Test void getAllRestaurantOrders_shouldReturnMappedOrders_whenRestaurantExistsAndHasOrders() {
        // Arrange
        Long restaurantId = 5L;
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();

        Order order1 = Order.builder()
                .orderId(201L)
                .restaurant(restaurant)
                .build();

        Order order2 = Order.builder()
                .orderId(202L)
                .restaurant(restaurant)
                .build();

        OrderResponse response1 = mock(OrderResponse.class);
        OrderResponse response2 = mock(OrderResponse.class);

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(orderRepository.findAllByRestaurant(restaurant)).thenReturn(List.of(order1, order2));
        when(orderMapper.toResponse(order1)).thenReturn(response1);
        when(orderMapper.toResponse(order2)).thenReturn(response2);

        // Act
        List<OrderResponse> result = orderService.getAllRestaurantOrders(restaurantId);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(List.of(response1, response2), result);

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(orderRepository, times(1)).findAllByRestaurant(restaurant);
        verify(orderMapper, times(1)).toResponse(order1);
        verify(orderMapper, times(1)).toResponse(order2);
    }

    @Test void getAllRestaurantOrders_shouldReturnEmptyList_whenRestaurantExistsWithNoOrders() {
        // Arrange
        Long restaurantId = 5L;
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();

        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(orderRepository.findAllByRestaurant(restaurant)).thenReturn(List.of());

        // Act
        List<OrderResponse> result = orderService.getAllRestaurantOrders(restaurantId);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(orderRepository, times(1)).findAllByRestaurant(restaurant);
        verify(orderMapper, never()).toResponse(any());
    }

    @Test void getAllRestaurantOrders_shouldThrowResourceNotFoundException_whenRestaurantDoesNotExist() {
        // Arrange
        Long restaurantId = 99L;
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.getAllRestaurantOrders(restaurantId)
        );

        // Verify
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verifyNoInteractions(orderRepository, orderMapper);
    }

}