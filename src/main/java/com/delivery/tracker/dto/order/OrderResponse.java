package com.delivery.tracker.dto.order;

import com.delivery.tracker.entity.DeliveryOrder;
import com.delivery.tracker.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id,OrderCustomerResponse customer,String address,List<OrderItemResponse> items,BigDecimal total,OrderStatus status,Instant createdAt) {
    public static OrderResponse from(DeliveryOrder order) {
        var u=order.getCustomer();
        return new OrderResponse(order.getId(),new OrderCustomerResponse(u.getId(),u.getName(),u.getEmail()),order.getAddress(),
            order.getItems().stream().map(i->new OrderItemResponse(i.getProductId(),i.getName(),i.getUnitPrice(),i.getQuantity(),i.getSubtotal())).toList(),
            order.getTotal(),order.getStatus(),order.getCreatedAt());
    }
}
