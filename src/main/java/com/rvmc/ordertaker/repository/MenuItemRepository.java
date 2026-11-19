package com.rvmc.ordertaker.repository;

import com.rvmc.ordertaker.entity.MenuItem;
import com.rvmc.ordertaker.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {
    List<MenuItem> findByRestaurantAndIsDeleted(Restaurant restaurant, Boolean isDeleted);

    Optional<MenuItem> findByMenuItemIdAndIsDeleted(Long menuItemId, Boolean isDeleted);

    List<MenuItem> findAllByMenuItemIdInAndIsDeleted(Collection<Long> menuItemIdList, Boolean isDeleted);
}
