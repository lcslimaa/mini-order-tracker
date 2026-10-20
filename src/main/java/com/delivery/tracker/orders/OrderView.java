package com.delivery.tracker.orders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderView(Long id,Customer customer,String address,List<Item> items,BigDecimal total,OrderStatus status,Instant createdAt) {
    public record Customer(Long id,String name,String email) {}
    public record Item(Long productId,String name,BigDecimal unitPrice,int quantity,BigDecimal subtotal) {}
    public static OrderView from(DeliveryOrder order) {
        var u=order.getCustomer();
        return new OrderView(order.getId(),new Customer(u.getId(),u.getName(),u.getEmail()),order.getAddress(),
            order.getItems().stream().map(i->new Item(i.getProductId(),i.getName(),i.getUnitPrice(),i.getQuantity(),i.getSubtotal())).toList(),
            order.getTotal(),order.getStatus(),order.getCreatedAt());
    }
}
