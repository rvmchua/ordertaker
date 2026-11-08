package com.rvmc.ordertaker.entity;

import com.rvmc.ordertaker.entity.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    private Customer customer;

    private Restaurant restaurant;

    private OrderStatus status;
    private BigDecimal totalAmount;

    @CreationTimestamp
    private ZonedDateTime createdAt;
}
