package com.rvmc.ordertaker.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

@Entity
@Table(name = "carts")
@Getter
@Setter
@Builder
@NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long cartId;

    private Customer customer;

    private Restaurant restaurant;

    private BigDecimal totalAmount;

    @CreationTimestamp
    private ZonedDateTime createdAt;
}
