package com.rvmc.ordertaker.service;

import com.rvmc.ordertaker.dto.*;
import com.rvmc.ordertaker.entity.*;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import com.rvmc.ordertaker.mapper.CartMapper;
import com.rvmc.ordertaker.repository.CartRepository;
import com.rvmc.ordertaker.repository.CustomerRepository;
import com.rvmc.ordertaker.repository.MenuItemRepository;
import com.rvmc.ordertaker.repository.RestaurantRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CustomerRepository customerRepository;
    private final MenuItemRepository menuItemRepository;
    private final CartMapper cartMapper;
    private final CartRepository cartRepository;
    private final RestaurantRepository restaurantRepository;

    public CartResponse getActiveCart(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", customerId));
        Cart activeCart = cartRepository.findByCustomerAndIsActive(customer, true)
                .orElseThrow(() -> new ResourceNotFoundException("No active cart found."));
        return cartMapper.toResponse(activeCart);
    }

    @Transactional
    public CartResponse createActiveCart(CreateCartRequest request) {
        // 1. Validate and merge requested menu items
        Map<Long, Integer> quantityByMenuItemId = normalizeQuantities(
                request.cartItems(),
                CartItemRequest::menuItemId,
                CartItemRequest::quantity,
                true
        );

        // 2. Retrieve customer and restaurant
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));
        Restaurant restaurant = restaurantRepository.findById(request.restaurantId())
                .orElseThrow(() -> new ResourceNotFoundException("Restaurant", request.restaurantId()));

        // 3. Prevent multiple active carts for the same customer
        if (cartRepository.existsByCustomerAndIsActive(customer, true)) {
            throw new IllegalStateException("Customer already has an active cart.");
        }

        // 4. Retrieve requested menu items
        List<Long> menuItemIds = List.copyOf(quantityByMenuItemId.keySet());
        List<MenuItem> menuItems = menuItemRepository
                .findAllByMenuItemIdInAndIsDeleted(menuItemIds, false);

        // 5. Validate menu items
        validateMenuItemsForRestaurant(
                menuItems,
                menuItemIds,
                restaurant.getRestaurantId()
        );

        // 6. Create a lookup map for menu items
        Map<Long, MenuItem> menuItemById = menuItems
                .stream()
                .collect(Collectors.toMap(
                        MenuItem::getMenuItemId,
                        Function.identity()
                ));

        // 7. Create cart
        Cart createdCart = Cart.create(restaurant, customer);

        // 8. Create cart items using the merged quantities
        quantityByMenuItemId.forEach((menuItemId, quantity) -> {
            if (quantity > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Quantity exceeds the maximum allowed value.");
            }

            CartItem cartItem = CartItem.builder()
                    .menuItem(menuItemById.get(menuItemId))
                    .quantity(quantity.shortValue())
                    .build();

            createdCart.addCartItem(cartItem);
        });

        // 9. Save cart
        Cart savedCart = cartRepository.save(createdCart);
        return cartMapper.toResponse(savedCart);
    }

    @Transactional
    public CartResponse updateActiveCart(UpdateCartRequest request) {
        // 1. Retrieve customer
        Customer customer = customerRepository.findById(request.customerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.customerId()));

        // 2. Retrieve active cart
        Cart activeCart = cartRepository.findByCustomerAndIsActive(customer, true)
                .orElseThrow(() -> new ResourceNotFoundException("No active cart found."));

        // 3. Prevent changing restaurant
        Long activeRestaurantId = activeCart.getRestaurant().getRestaurantId();

        if (request.restaurantId() != null && !activeRestaurantId.equals(request.restaurantId())) {
            throw new IllegalArgumentException("Cannot change restaurant on an active cart.");
        }

        // 4. Validate and merge requested menu items
        // An empty list is allowed here to clear the cart.
        Map<Long, Integer> quantityByMenuItemId = normalizeQuantities(
                request.cartItems(),
                CartItemUpdateRequest::menuItemId,
                CartItemUpdateRequest::quantity,
                false
        );

        // 5. Retrieve menu items, if the request is not empty
        List<Long> menuItemIds = List.copyOf(quantityByMenuItemId.keySet());
        List<MenuItem> menuItems;

        if (menuItemIds.isEmpty()) {
            menuItems = List.of();
        } else {
            menuItems = menuItemRepository.findAllByMenuItemIdInAndIsDeleted(
                    menuItemIds, false
            );
        }

        // 6. Validate menu items against the active cart's restaurant
        validateMenuItemsForRestaurant(
                menuItems,
                menuItemIds,
                activeRestaurantId
        );

        // 7. Create a lookup map
        Map<Long, MenuItem> menuItemById = menuItems
                .stream()
                .collect(Collectors.toMap(
                        MenuItem::getMenuItemId,
                        Function.identity()
                ));

        // 8. Build replacement cart items
        List<CartItem> updatedCartItems = quantityByMenuItemId.entrySet()
                .stream()
                .map(entry -> CartItem.builder()
                        .menuItem(menuItemById.get(entry.getKey()))
                        .quantity(entry.getValue().shortValue())
                        .build())
                .toList();

        // 9. Replace existing cart items
        activeCart.replaceCartItems(updatedCartItems);

        // 10. Save updated cart
        Cart savedCart = cartRepository.save(activeCart);

        return cartMapper.toResponse(savedCart);
    }

    /*
     * Validation Helper Functions
     */

    private <T> Map<Long, Integer> normalizeQuantities(
            List<T> items,
            Function<T, Long> menuItemIdExtractor,
            Function<T, ? extends Number> quantityExtractor,
            boolean requireNonEmpty
    ) {

        if (items == null) {
            throw new IllegalArgumentException("Cart items cannot be null.");
        }

        if (requireNonEmpty && items.isEmpty()) {
            throw new IllegalArgumentException("Cart must contain at least one item.");
        }

        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (T item : items) {
            if (item == null) {
                throw new IllegalArgumentException("Cart item cannot be null.");
            }

            Long menuItemId = menuItemIdExtractor.apply(item);
            Integer quantity = (Integer) quantityExtractor.apply(item);

            if (menuItemId == null) {
                throw new IllegalArgumentException("Menu item ID cannot be null.");
            }

            if (quantity == null || quantity <= 0) {
                throw new IllegalArgumentException("Quantity must be greater than zero.");
            }

            try {
                quantities.merge(menuItemId, quantity, Math::addExact);
            } catch (ArithmeticException exception) {
                throw new IllegalArgumentException("Combined item quantity exceeds the maximum supported value.", exception);
            }
        }
        return quantities;
    }

    private void validateMenuItemsForRestaurant(
            List<MenuItem> menuItems,
            List<Long> requestedMenuItemIds,
            Long restaurantId
    ) {
        // Check whether all requested menu items were found
        if (menuItems.size() != requestedMenuItemIds.size()) {
            Set<Long> foundIds = menuItems.stream()
                    .map(MenuItem::getMenuItemId)
                    .collect(Collectors.toSet());
            List<Long> missingIds = requestedMenuItemIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new ResourceNotFoundException("Menu items not found for IDs: " + missingIds);
        }

        // Check whether menu items belong to the selected restaurant
        boolean invalidRestaurantItems = menuItems.stream()
                .anyMatch(item ->
                        !Objects.equals(
                                item.getRestaurant().getRestaurantId(),
                                restaurantId
                        )
                );

        if (invalidRestaurantItems) {
            throw new IllegalArgumentException("One or more menu items do not belong to this restaurant.");
        }
    }
}