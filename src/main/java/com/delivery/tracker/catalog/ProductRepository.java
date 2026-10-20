package com.delivery.tracker.catalog;

import java.util.*;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;

public interface ProductRepository extends JpaRepository<Product,Long> {
    List<Product> findAllByOrderByIdAsc();
    List<Product> findByAvailableTrueOrderByIdAsc();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findLockedById(Long id);
}
