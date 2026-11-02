package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
}
