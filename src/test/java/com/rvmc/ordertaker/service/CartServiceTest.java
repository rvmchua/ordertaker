package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.CartItemRequest;
import com.rvmc.ordertaker.dto.CartItemUpdateRequest;
import com.rvmc.ordertaker.dto.CartResponse;
import com.rvmc.ordertaker.dto.CreateCartRequest;
import com.rvmc.ordertaker.dto.UpdateCartRequest;
import com.rvmc.ordertaker.entity.Cart;
import com.rvmc.ordertaker.entity.CartItem;
import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.CartMapper;
import com.rvmc.ordertaker.repository.CartRepository;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.MenuItemRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock private CustomerRepository customerRepository;
    @Mock private MenuItemRepository menuItemRepository;
    @Mock private CartMapper cartMapper;
    @Mock private CartRepository cartRepository;
    @Mock private RestaurantRepository restaurantRepository;
    @InjectMocks private CartService cartService;

    // -------------------------------------------------------------------------
    // getActiveCart
    // -------------------------------------------------------------------------

    @Test
    void getActiveCart_shouldReturnMappedCart_whenCustomerAndActiveCartExist() {
        Long customerId = 1L;
        Customer customer = Customer.builder().customerId(customerId).build();
        Cart activeCart = Cart.builder().cartId(10L).customer(customer).isActive(true).build();
        CartResponse expected = mock(CartResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(cartMapper.toResponse(activeCart)).thenReturn(expected);

        CartResponse actual = cartService.getActiveCart(customerId);

        assertEquals(expected, actual);
        verify(cartRepository, times(1)).findByCustomerAndIsActive(customer, true);
        verify(cartMapper, times(1)).toResponse(activeCart);
    }

    @Test
    void getActiveCart_shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        Long customerId = 99L;
        when(customerRepository.findById(customerId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> cartService.getActiveCart(customerId));
        verifyNoInteractions(cartRepository, cartMapper);
    }

    @Test
    void getActiveCart_shouldThrowResourceNotFoundException_whenNoActiveCartExists() {
        Long customerId = 1L;
        Customer customer = Customer.builder().customerId(customerId).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.getActiveCart(customerId)
        );
        assertEquals("No active cart found.", ex.getMessage());
        verifyNoInteractions(cartMapper);
    }

    // -------------------------------------------------------------------------
    // createActiveCart
    // -------------------------------------------------------------------------

    @Test
    void createActiveCart_shouldMergeDuplicateItemsSaveCartAndReturnResponse_whenValid() {
        Long customerId = 1L;
        Long restaurantId = 5L;

        CreateCartRequest request = new CreateCartRequest(
                customerId,
                restaurantId,
                List.of(
                        new CartItemRequest(10L, 2),
                        new CartItemRequest(10L, 3)
                )
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(restaurantId).build();
        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(150.00))
                .isDeleted(false)
                .build();

        CartResponse expectedResponse = mock(CartResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cartRepository.existsByCustomerAndIsActive(customer, true)).thenReturn(false);
        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(List.of(10L), false))
                .thenReturn(List.of(item10));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(expectedResponse);

        CartResponse actual = cartService.createActiveCart(request);

        assertEquals(expectedResponse, actual);

        ArgumentCaptor<Cart> cartCaptor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, times(1)).save(cartCaptor.capture());

        Cart savedCart = cartCaptor.getValue();
        assertEquals(customer, savedCart.getCustomer());
        assertEquals(restaurant, savedCart.getRestaurant());
        assertEquals(1, savedCart.getCartItems().size());
        assertEquals((short) 5, savedCart.getCartItems().getFirst().getQuantity());
    }

    @Test
    void createActiveCart_shouldThrowIllegalArgumentException_whenCartItemsEmpty() {
        CreateCartRequest request = new CreateCartRequest(1L, 5L, List.of());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.createActiveCart(request)
        );
        assertEquals("Cart must contain at least one item.", ex.getMessage());
        verifyNoInteractions(customerRepository, restaurantRepository, cartRepository, menuItemRepository, cartMapper);
    }

    @Test
    void createActiveCart_shouldThrowIllegalStateException_whenCustomerAlreadyHasActiveCart() {
        Long customerId = 1L;
        Long restaurantId = 5L;

        CreateCartRequest request = new CreateCartRequest(
                customerId,
                restaurantId,
                List.of(new CartItemRequest(10L, 1))
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(restaurantId).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cartRepository.existsByCustomerAndIsActive(customer, true)).thenReturn(true);

        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> cartService.createActiveCart(request)
        );
        assertEquals("Customer already has an active cart.", ex.getMessage());
        verifyNoInteractions(menuItemRepository, cartMapper);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void createActiveCart_shouldThrowIllegalArgumentException_whenQuantityExceedsShortMaxValue() {
        Long customerId = 1L;
        Long restaurantId = 5L;
        int tooLarge = Short.MAX_VALUE + 1;

        CreateCartRequest request = new CreateCartRequest(
                customerId,
                restaurantId,
                List.of(new CartItemRequest(10L, tooLarge))
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(restaurantId).build();
        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .isDeleted(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cartRepository.existsByCustomerAndIsActive(customer, true)).thenReturn(false);
        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(List.of(10L), false))
                .thenReturn(List.of(item10));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.createActiveCart(request)
        );
        assertEquals("Quantity exceeds the maximum allowed value.", ex.getMessage());
        verify(cartRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // updateActiveCart
    // -------------------------------------------------------------------------

    @Test
    void updateActiveCart_shouldClearCartItems_whenRequestCartItemsIsEmpty() {
        Long customerId = 1L;
        Long restaurantId = 5L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                restaurantId,
                List.of()
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(restaurantId).build();
        Cart activeCart = Cart.builder()
                .cartId(20L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(new ArrayList<>(List.of(mock(CartItem.class))))
                .build();

        CartResponse expectedResponse = mock(CartResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(cartRepository.save(activeCart)).thenReturn(activeCart);
        when(cartMapper.toResponse(activeCart)).thenReturn(expectedResponse);

        CartResponse actual = cartService.updateActiveCart(request);

        assertEquals(expectedResponse, actual);
        assertTrue(activeCart.getCartItems().isEmpty());
        verify(menuItemRepository, never()).findAllByMenuItemIdInAndIsDeleted(any(), anyBoolean());
        verify(cartRepository, times(1)).save(activeCart);
    }

    @Test
    void updateActiveCart_shouldThrowIllegalArgumentException_whenRestaurantIdDiffersFromActiveCart() {
        Long customerId = 1L;
        Long activeRestaurantId = 5L;
        Long requestedRestaurantId = 99L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                requestedRestaurantId,
                List.of(new CartItemUpdateRequest(10L, (short) 1))
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant activeRestaurant = Restaurant.builder().restaurantId(activeRestaurantId).build();
        Cart activeCart = Cart.builder()
                .cartId(20L)
                .customer(customer)
                .restaurant(activeRestaurant)
                .isActive(true)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateActiveCart(request)
        );
        assertEquals("Cannot change restaurant on an active cart.", ex.getMessage());
        verifyNoInteractions(menuItemRepository, cartMapper);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateActiveCart_shouldThrowIllegalArgumentException_whenMenuItemBelongsToDifferentRestaurant_whenMenuItemBelongsToDifferentRestaurant() {
        Long customerId = 1L;
        Long activeRestaurantId = 5L;
        Long otherRestaurantId = 77L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                null,
                List.of(new CartItemUpdateRequest(10L, (short) 1))
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant activeRestaurant = Restaurant.builder().restaurantId(activeRestaurantId).build();
        Restaurant otherRestaurant = Restaurant.builder().restaurantId(otherRestaurantId).build();
        Cart activeCart = Cart.builder()
                .cartId(20L)
                .customer(customer)
                .restaurant(activeRestaurant)
                .isActive(true)
                .build();

        MenuItem foreignItem = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(otherRestaurant)
                .isDeleted(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(List.of(10L), false))
                .thenReturn(List.of(foreignItem));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateActiveCart(request)
        );
        assertEquals("One or more menu items do not belong to this restaurant.", ex.getMessage());
        verify(cartRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Missing Customer & Missing Active Cart
    // -------------------------------------------------------------------------

    @Test
    void createActiveCart_shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        Long missingCustomerId = 404L;
        Long restaurantId = 5L;

        CreateCartRequest request = new CreateCartRequest(
                missingCustomerId,
                restaurantId,
                List.of(new CartItemRequest(10L, 1))
        );

        when(customerRepository.findById(missingCustomerId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.createActiveCart(request)
        );

        verify(customerRepository, times(1)).findById(missingCustomerId);
        verifyNoInteractions(restaurantRepository, cartRepository, menuItemRepository, cartMapper);
    }

    @Test
    void updateActiveCart_shouldThrowResourceNotFoundException_whenCustomerDoesNotExist() {
        Long missingCustomerId = 404L;

        UpdateCartRequest request = new UpdateCartRequest(
                missingCustomerId,
                null,
                List.of(new CartItemUpdateRequest(10L, (short) 1))
        );

        when(customerRepository.findById(missingCustomerId)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateActiveCart(request)
        );

        verify(customerRepository, times(1)).findById(missingCustomerId);
        verifyNoInteractions(cartRepository, menuItemRepository, cartMapper);
    }

    @Test
    void updateActiveCart_shouldThrowResourceNotFoundException_whenActiveCartDoesNotExist() {
        Long customerId = 1L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                null,
                List.of(new CartItemUpdateRequest(10L, (short) 2))
        );

        Customer customer = Customer.builder().customerId(customerId).build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(
                ResourceNotFoundException.class,
                () -> cartService.updateActiveCart(request)
        );

        assertEquals("No active cart found.", ex.getMessage());
        verify(cartRepository, times(1)).findByCustomerAndIsActive(customer, true);
        verifyNoInteractions(menuItemRepository, cartMapper);
    }

    // -------------------------------------------------------------------------
    // Restaurant Mismatch (Request Restaurant & MenuItem Restaurant)
    // -------------------------------------------------------------------------

    @Test
    void updateActiveCart_shouldThrowIllegalArgumentException_whenRequestRestaurantDiffersFromActiveCart() {
        Long customerId = 1L;
        Long activeRestaurantId = 5L;
        Long conflictingRestaurantId = 8L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                conflictingRestaurantId,
                List.of(new CartItemUpdateRequest(10L, (short) 1))
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant activeRestaurant = Restaurant.builder().restaurantId(activeRestaurantId).build();
        Cart activeCart = Cart.builder()
                .cartId(30L)
                .customer(customer)
                .restaurant(activeRestaurant)
                .isActive(true)
                .cartItems(new ArrayList<>())
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateActiveCart(request)
        );

        assertEquals("Cannot change restaurant on an active cart.", ex.getMessage());
        verifyNoInteractions(menuItemRepository, cartMapper);
        verify(cartRepository, never()).save(any());
    }

    @Test
    void createActiveCart_shouldThrowIllegalArgumentException_whenMenuItemBelongsToDifferentRestaurant() {
        Long customerId = 1L;
        Long requestedRestaurantId = 5L;
        Long otherRestaurantId = 99L;

        CreateCartRequest request = new CreateCartRequest(
                customerId,
                requestedRestaurantId,
                List.of(new CartItemRequest(10L, 1))
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant requestedRestaurant = Restaurant.builder().restaurantId(requestedRestaurantId).build();
        Restaurant otherRestaurant = Restaurant.builder().restaurantId(otherRestaurantId).build();

        MenuItem wrongRestaurantItem = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(otherRestaurant)
                .isDeleted(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(restaurantRepository.findById(requestedRestaurantId)).thenReturn(Optional.of(requestedRestaurant));
        when(cartRepository.existsByCustomerAndIsActive(customer, true)).thenReturn(false);
        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(List.of(10L), false))
                .thenReturn(List.of(wrongRestaurantItem));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.createActiveCart(request)
        );

        assertEquals("One or more menu items do not belong to this restaurant.", ex.getMessage());
        verify(cartRepository, never()).save(any());
    }

    @Test
    void updateActiveCart_shouldThrowIllegalArgumentException_whenMenuItemBelongsToDifferentRestaurant() {
        Long customerId = 1L;
        Long activeRestaurantId = 5L;
        Long otherRestaurantId = 99L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                null,
                List.of(new CartItemUpdateRequest(10L, (short) 2))
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant activeRestaurant = Restaurant.builder().restaurantId(activeRestaurantId).build();
        Restaurant otherRestaurant = Restaurant.builder().restaurantId(otherRestaurantId).build();

        Cart activeCart = Cart.builder()
                .cartId(30L)
                .customer(customer)
                .restaurant(activeRestaurant)
                .isActive(true)
                .cartItems(new ArrayList<>())
                .build();

        MenuItem wrongRestaurantItem = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(otherRestaurant)
                .isDeleted(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(List.of(10L), false))
                .thenReturn(List.of(wrongRestaurantItem));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.updateActiveCart(request)
        );

        assertEquals("One or more menu items do not belong to this restaurant.", ex.getMessage());
        verify(cartRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Duplicate Quantities Merging
    // -------------------------------------------------------------------------

    @Test
    void updateActiveCart_shouldMergeDuplicateMenuItemQuantities_whenRequestHasRepeatedItems() {
        Long customerId = 1L;
        Long restaurantId = 5L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                restaurantId,
                List.of(
                        new CartItemUpdateRequest(10L, (short) 2),
                        new CartItemUpdateRequest(10L, (short) 4)
                )
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(restaurantId).build();
        Cart activeCart = Cart.builder()
                .cartId(30L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(new ArrayList<>())
                .build();

        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .price(BigDecimal.valueOf(120.00))
                .isDeleted(false)
                .build();

        CartResponse expectedResponse = mock(CartResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(List.of(10L), false))
                .thenReturn(List.of(item10));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(cartMapper.toResponse(any(Cart.class))).thenReturn(expectedResponse);

        CartResponse actual = cartService.updateActiveCart(request);

        assertEquals(expectedResponse, actual);

        ArgumentCaptor<Cart> captor = ArgumentCaptor.forClass(Cart.class);
        verify(cartRepository, times(1)).save(captor.capture());

        Cart savedCart = captor.getValue();
        assertEquals(1, savedCart.getCartItems().size());
        assertEquals((short) 6, savedCart.getCartItems().get(0).getQuantity());
        assertEquals(10L, savedCart.getCartItems().get(0).getMenuItem().getMenuItemId());
    }

    // -------------------------------------------------------------------------
    // Overflow Handling (Integer Max & Short Max)
    // -------------------------------------------------------------------------

    @Test
    void createActiveCart_shouldThrowIllegalArgumentException_whenMergedQuantityOverflowsInteger() {
        CreateCartRequest request = new CreateCartRequest(
                1L,
                5L,
                List.of(
                        new CartItemRequest(10L, Integer.MAX_VALUE),
                        new CartItemRequest(10L, 1)
                )
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.createActiveCart(request)
        );

        assertEquals("Combined item quantity exceeds the maximum supported value.", ex.getMessage());
        verifyNoInteractions(customerRepository, restaurantRepository, cartRepository, menuItemRepository, cartMapper);
    }

    @Test
    void createActiveCart_shouldThrowIllegalArgumentException_whenMergedQuantityExceedsShortMaxValue() {
        Long customerId = 1L;
        Long restaurantId = 5L;

        CreateCartRequest request = new CreateCartRequest(
                customerId,
                restaurantId,
                List.of(
                        new CartItemRequest(10L, (int) Short.MAX_VALUE),
                        new CartItemRequest(10L, 1)
                )
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(restaurantId).build();
        MenuItem item10 = MenuItem.builder()
                .menuItemId(10L)
                .restaurant(restaurant)
                .isDeleted(false)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cartRepository.existsByCustomerAndIsActive(customer, true)).thenReturn(false);
        when(menuItemRepository.findAllByMenuItemIdInAndIsDeleted(List.of(10L), false))
                .thenReturn(List.of(item10));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> cartService.createActiveCart(request)
        );

        assertEquals("Quantity exceeds the maximum allowed value.", ex.getMessage());
        verify(cartRepository, never()).save(any());
    }

    // -------------------------------------------------------------------------
    // Clearing Cart Items via Empty Update List
    // -------------------------------------------------------------------------

    @Test
    void updateActiveCart_shouldClearExistingItems_whenCartItemsListIsEmpty() {
        Long customerId = 1L;
        Long restaurantId = 5L;

        UpdateCartRequest request = new UpdateCartRequest(
                customerId,
                null,
                List.of()
        );

        Customer customer = Customer.builder().customerId(customerId).build();
        Restaurant restaurant = Restaurant.builder().restaurantId(restaurantId).build();
        CartItem existingItem = CartItem.builder().quantity((short) 2).build();
        Cart activeCart = Cart.builder()
                .cartId(30L)
                .customer(customer)
                .restaurant(restaurant)
                .isActive(true)
                .cartItems(new ArrayList<>(List.of(existingItem)))
                .build();

        CartResponse expectedResponse = mock(CartResponse.class);

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(cartRepository.findByCustomerAndIsActive(customer, true)).thenReturn(Optional.of(activeCart));
        when(cartRepository.save(activeCart)).thenReturn(activeCart);
        when(cartMapper.toResponse(activeCart)).thenReturn(expectedResponse);

        CartResponse actual = cartService.updateActiveCart(request);

        assertEquals(expectedResponse, actual);
        assertTrue(activeCart.getCartItems().isEmpty());
        verifyNoInteractions(menuItemRepository);
        verify(cartRepository, times(1)).save(activeCart);
        verify(cartMapper, times(1)).toResponse(activeCart);
    }

    // -------------------------------------------------------------------------
    // Customer Cannot Create a Second Active Cart
    // -------------------------------------------------------------------------

    @Test
    void createActiveCart_shouldThrowIllegalStateException_whenCustomerAlreadyHasAnActiveCart() {
        // Arrange
        Long customerId = 1L;
        Long restaurantId = 5L;

        CreateCartRequest request = new CreateCartRequest(
                customerId,
                restaurantId,
                List.of(new CartItemRequest(10L, 2))
        );

        Customer customer = Customer.builder()
                .customerId(customerId)
                .build();

        Restaurant restaurant = Restaurant.builder()
                .restaurantId(restaurantId)
                .build();

        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customer));
        when(restaurantRepository.findById(restaurantId)).thenReturn(Optional.of(restaurant));
        when(cartRepository.existsByCustomerAndIsActive(customer, true)).thenReturn(true);

        // Act & Assert
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> cartService.createActiveCart(request)
        );
        assertEquals("Customer already has an active cart.", exception.getMessage());

        // Verify
        verify(customerRepository, times(1)).findById(customerId);
        verify(restaurantRepository, times(1)).findById(restaurantId);
        verify(cartRepository, times(1)).existsByCustomerAndIsActive(customer, true);
        verify(cartRepository, never()).save(any());
        verifyNoInteractions(menuItemRepository, cartMapper);
    }

}