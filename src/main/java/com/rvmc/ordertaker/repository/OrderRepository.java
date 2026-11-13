package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.Customer;
import com.rvmc.ordertaker.entity.Order;
import com.rvmc.ordertaker.entity.Restaurant;
import com.rvmc.ordertaker.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderIdAndStatusNotIn(Long orderId, List<OrderStatus> orderStatuses);

    Optional<Order> findByOrderIdAndCustomer(Long orderId, Customer customer);

    Optional<Order> findByOrderIdAndRestaurant(Long orderId, Restaurant restaurant);

    List<Order> findAllByCustomer(Customer customer);

    List<Order> findAllByRestaurant(Restaurant restaurant);
}
