package com.rvmc.ordertaker.entity;

import com.rvmc.ordertaker.entity.enums.OrderStatus;
import com.rvmc.ordertaker.exception.ResourceNotFoundException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    private BigDecimal totalAmount;

    @CreationTimestamp
    private ZonedDateTime createdAt;

    @Version
    private Long version;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

//    Domain factory method for creating Order object from MenuItem list
public static Order create(Cart cart) {
    if (cart == null) {
        throw new IllegalArgumentException("Cart cannot be null.");
    }
    if (cart.getCustomer() == null) {
        throw new IllegalArgumentException("Cart customer cannot be null.");
    }
    if (cart.getRestaurant() == null) {
        throw new IllegalArgumentException("Cart restaurant cannot be null.");
    }
    if (cart.getCartItems() == null || cart.getCartItems().isEmpty()) {
        throw new IllegalStateException("Cannot create an order from an empty cart.");
    }

    BigDecimal totalPrice = BigDecimal.ZERO;
    List<OrderItem> orderItems = new ArrayList<>(cart.getCartItems().size());

    Order order = Order.builder()
            .restaurant(cart.getRestaurant())
            .customer(cart.getCustomer())
            .status(OrderStatus.PENDING)
            .build();

    for (CartItem cartItem : cart.getCartItems()) {
        if (cartItem == null) {
            throw new IllegalArgumentException("Cart item cannot be null.");
        }

        MenuItem menuItem = cartItem.getMenuItem();
        if (menuItem == null) {
            throw new IllegalArgumentException("Cart item menu item cannot be null.");
        }
        if (menuItem.getPrice() == null) {
            throw new IllegalArgumentException("Menu item price cannot be null.");
        }
        if (menuItem.getIsAvailable() == false) {
            throw new ResourceNotFoundException("Menu item is currently unavailable.");
        }
        if (cartItem.getQuantity() <= 0) {
            throw new IllegalArgumentException("Cart item quantity must be greater than zero.");
        }

        BigDecimal quantity = BigDecimal.valueOf(cartItem.getQuantity());
        BigDecimal itemTotal = menuItem.getPrice().multiply(quantity);
        totalPrice = totalPrice.add(itemTotal);

        OrderItem orderItem = OrderItem.builder()
                .order(order)
                .menuItem(menuItem)
                .quantity(cartItem.getQuantity())
                .priceAtOrderTime(menuItem.getPrice())
                .build();

        orderItems.add(orderItem);
    }

    order.setTotalAmount(totalPrice);
    order.setOrderItems(orderItems);

    return order;
}
}
