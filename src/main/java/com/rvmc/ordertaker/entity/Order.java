package com.rvmc.ordertaker.entity;

import com.rvmc.ordertaker.dto.OrderItemRequest;
import com.rvmc.ordertaker.dto.OrderRequest;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    public static Order create(Restaurant restaurant, Customer customer, List<MenuItem> menuItems, OrderRequest request) {
        Order order = Order.builder()
                .restaurant(restaurant)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .build();

        Map<Long, MenuItem> menuItemMap = menuItems.stream()
                .collect(Collectors.toMap(MenuItem::getMenuItemId, Function.identity()));

        BigDecimal totalPrice = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest requestItem : request.orderItems()) {
            MenuItem menuItem = menuItemMap.get(requestItem.menuItemId());

            BigDecimal itemTotal = menuItem.getPrice().multiply(BigDecimal.valueOf(requestItem.quantity()));
            totalPrice = totalPrice.add(itemTotal);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .menuItem(menuItem)
                    .quantity(requestItem.quantity())
                    .priceAtOrderTime(menuItem.getPrice())
                    .build();

            orderItems.add(orderItem);
        }

        order.setTotalAmount(totalPrice);
        order.setOrderItems(orderItems);

        return order;
    }
}
