package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository <Customer, Long> {
}
