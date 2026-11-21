package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.OrderResponse;
import com.rvmc.ordertaker.dto.UpdateOrderStatus;
import com.rvmc.ordertaker.entity.*;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.exception.OrderTaggedAsReceivedOrCancelledException;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.OrderMapper;
import com.rvmc.ordertaker.repository.CartRepository;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.OrderRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private CustomerRepository customerRepository;
    @Mock private CartRepository cartRepository;
    @Mock private CartService cartService;
    @Mock private OrderMapper orderMapper;
    @Mock private OrderRepository orderRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @InjectMocks private OrderService orderService;

    // -------------------------------------------------------------------------
    // createOrder
    // -------------------------------------------------------------------------

    @Test
    void createOrder_shouldSaveOrderMapResponseAndDeactivateCart_whenActiveCartIsValid() {
        // Arrange
        Long customerId = 1L;
        Long restaurantId = 5L;

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .restaurantName("Burger Joint")
                .build();

        MenuItem burger = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .itemName("Cheeseburger")
                .price(BigDecimal.valueOf(250.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        MenuItem fries = MenuItem.builder()
                .menuItemId(11L)
                .restaurant(restaurant)
                .itemName("Fries")
                .price(BigDecimal.valueOf(120.00))
                .isAvailable(true)
                .isDeleted(false)
                .build();

        List<CartItem> items = List.of(
                CartItem.builder().menuItem(burger).quantity((short) 2).build(),
                CartItem.builder().menuItem(fries).quantity((short) 1).build()
        );

        Cart activeCart = Cart.builder()
                .cartId(70L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(items)
                .build();

        Cart deactivatedCart = Cart.builder()
                .cartId(70L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(false)
                .cartItems(items)
                .build();

        OrderResponse expectedResponse = mock(OrderResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderMapper.toResponse(any(Order.class))).thenReturn(expectedResponse);
        when(cartService.deactivateCart(activeCart)).thenReturn(deactivatedCart);

        // Act
        OrderResponse result = orderService.createOrder(customerId);

        // Assert
        assertNotNull(result);
        assertEquals(expectedResponse, result);

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();
        assertEquals(customer, savedOrder.getCustomer());
        assertEquals(restaurant, savedOrder.getRestaurant());
        assertEquals(OrderStatus.PENDING, savedOrder.getStatus());
        assertEquals(0, BigDecimal.valueOf(620.00).compareTo(savedOrder.getTotalAmount()));
        assertEquals(2, savedOrder.getOrderItems().size());
        assertEquals(BigDecimal.valueOf(250.00), savedOrder.getOrderItems().getFirst().getPriceAtOrderTime());
        assertEquals((short) 2, savedOrder.getOrderItems().getFirst().getQuantity());
        assertEquals(savedOrder, savedOrder.getOrderItems().getFirst().getOrder());

        InOrder inOrder = inOrder(orderRepository, orderMapper, cartService);
        inOrder.verify(orderRepository).save(savedOrder);
        inOrder.verify(orderMapper).toResponse(savedOrder);
        inOrder.verify(cartService).deactivateCart(activeCart);
    }

    @Test
    void createOrder_shouldThrowResourceNotFoundException_whenCartItemsIsNull() {
        // Arrange
        Long customerId = 1L;
        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();
        Cart activeCart = Cart.builder()
                .cartId(70L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(null)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));

        // Act & Assert
        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> orderService.createOrder(customerId)
        );
        assertEquals("Cart cannot be empty.", exception.getMessage());

        verifyNoInteractions(orderRepository, orderMapper, cartService);
    }

    @Test
    void createOrder_shouldThrowIllegalStateException_whenActiveCartItemsListIsEmpty() {
        // Arrange
        Long customerId = 1L;
        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();
        Cart activeCart = Cart.builder()
                .cartId(70L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(List.of())
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(customerId)
        );
        assertEquals("Cannot create an order from an empty cart.", exception.getMessage());

        verifyNoInteractions(orderRepository, orderMapper, cartService);
    }

    @Test
    void createOrder_shouldThrowIllegalArgumentException_whenMenuItemPriceIsNull() {
        // Arrange
        Long customerId = 1L;
        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();

        MenuItem itemWithoutPrice = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(null)
                .build();

        CartItem cartItem = CartItem.builder()
                .menuItem(itemWithoutPrice)
                .quantity((short) 1)
                .build();

        Cart activeCart = Cart.builder()
                .cartId(70L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(List.of(cartItem))
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createOrder(customerId)
        );
        assertEquals("Menu item price cannot be null.", exception.getMessage());

        verifyNoInteractions(orderRepository, orderMapper, cartService);
    }

    @Test
    void createOrder_shouldThrowIllegalStateException_whenDataIntegrityViolationOccursOnSave() {
        // Arrange
        Long customerId = 1L;
        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();

        MenuItem item = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(150.00))
                .build();

        CartItem cartItem = CartItem.builder()
                .menuItem(item)
                .quantity((short) 1)
                .build();

        Cart activeCart = Cart.builder()
                .cartId(70L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(List.of(cartItem))
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate order reference"));

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(customerId)
        );
        assertEquals("Order could not be saved due to a data constraint violation.", exception.getMessage());

        verify(orderRepository, times(1)).save(any(Order.class));
        verifyNoInteractions(orderMapper, cartService);
    }

    @Test
    void createOrder_shouldThrowIllegalStateException_whenGenericDataAccessExceptionOccursOnSave() {
        // Arrange
        Long customerId = 1L;
        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(5L).build();

        MenuItem item = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(150.00))
                .build();

        CartItem cartItem = CartItem.builder()
                .menuItem(item)
                .quantity((short) 1)
                .build();

        Cart activeCart = Cart.builder()
                .cartId(70L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(List.of(cartItem))
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(orderRepository.save(any(Order.class)))
                .thenThrow(new DataAccessException("Database connection lost") {});

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> orderService.createOrder(customerId)
        );
        assertEquals("Failed to save order due to a persistence error.", exception.getMessage());

        verify(orderRepository, times(1)).save(any(Order.class));
        verifyNoInteractions(orderMapper, cartService);
    }

    // -------------------------------------------------------------------------
    // updateOrder
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // getCustomerOrderById
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // getRestaurantOrderById
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // getAllCustomerOrders
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // getAllRestaurantOrders
    // -------------------------------------------------------------------------

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