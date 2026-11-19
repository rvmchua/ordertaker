package com.rvmc.ordertaker.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "carts")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    @OneToOne
    @JoinColumn(name = "customer_id", unique = true, nullable = false)
    private Customer customer;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    private BigDecimal totalAmount;

    @CreationTimestamp
    private ZonedDateTime createdAt;

    private Boolean isActive;

    @Builder.Default
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> cartItems = new ArrayList<>();

    public void addCartItem(CartItem cartItem) {
        cartItems.add(cartItem);
        cartItem.setCart(this);
    }

    public void replaceCartItems(List<CartItem> newItems) {
        cartItems.clear();

        for (CartItem item : newItems) {
            addCartItem(item);
        }
    }

    //    Domain factory method for creating Cart object from MenuItem list
    public static Cart create(Restaurant restaurant, Customer customer) {
        return Cart.builder()
                .restaurant(restaurant)
                .customer(customer)
                .isActive(true)
                .cartItems(new ArrayList<>())
                .build();
    }
}
