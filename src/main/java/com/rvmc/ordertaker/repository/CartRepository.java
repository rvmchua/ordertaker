package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.Cart;
import com.rvmc.ordertaker.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomerAndIsActive(Customer customer, boolean isActive);

    Boolean existsByCustomerAndIsActive(Customer customer, boolean isActive);
}
