package com.delivery.tracker.orders;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderRepository extends JpaRepository<DeliveryOrder,Long> {
    Optional<DeliveryOrder> findByIdAndCustomerId(Long id,Long customerId);
    List<DeliveryOrder> findByCustomerIdOrderByIdAsc(Long customerId);
    List<DeliveryOrder> findAllByOrderByIdAsc();
}
