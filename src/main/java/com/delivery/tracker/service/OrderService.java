package com.delivery.tracker.service;

import com.delivery.tracker.dto.order.CreateOrderItemRequest;
import com.delivery.tracker.dto.order.CreateOrderRequest;
import com.delivery.tracker.dto.order.OrderResponse;
import com.delivery.tracker.entity.DeliveryOrder;
import com.delivery.tracker.entity.Product;
import com.delivery.tracker.enums.OrderStatus;
import com.delivery.tracker.exception.ApiException;
import com.delivery.tracker.repository.OrderRepository;
import com.delivery.tracker.repository.ProductRepository;
import com.delivery.tracker.repository.UserRepository;
import com.delivery.tracker.security.Identity;

import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    public OrderService(OrderRepository orders,ProductRepository products,UserRepository users) { this.orders=orders;this.products=products;this.users=users; }
    @Transactional
    public OrderResponse create(Identity identity,CreateOrderRequest body) {
        var ids=body.items().stream().map(CreateOrderItemRequest::productId).toList();
        if(new HashSet<>(ids).size()!=ids.size()) throw ApiException.invalid();
        var snapshot=new HashMap<Long,Product>();
        // Keep locks until commit; stable order avoids deadlocks across overlapping baskets.
        for(Long id : ids.stream().sorted().toList()) {
            var p=products.findLockedById(id).orElseThrow(ApiException::invalid);
            if(!p.isAvailable()) throw ApiException.invalid();
            snapshot.put(id,p);
        }
        var customer=users.findById(identity.id()).orElseThrow(ApiException::notFound);
        var order=new DeliveryOrder(customer,body.address());
        for(var item : body.items()) order.addItem(snapshot.get(item.productId()),item.quantity());
        return OrderResponse.from(orders.saveAndFlush(order));
    }
    @Transactional(readOnly=true)
    public List<OrderResponse> list(Identity identity) {
        return (identity.operator()?orders.findAllByOrderByIdAsc():orders.findByCustomerIdOrderByIdAsc(identity.id())).stream().map(OrderResponse::from).toList();
    }
    @Transactional(readOnly=true)
    public OrderResponse get(Long id,Identity identity) { return OrderResponse.from(visible(id,identity)); }
    @Transactional
    public OrderResponse changeStatus(Long id,Identity identity,OrderStatus target) {
        var order=visible(id,identity);order.advance(target);orders.flush();
        return OrderResponse.from(order);
    }
    @Transactional
    public OrderResponse cancel(Long id,Identity identity) {
        var order=visible(id,identity);order.cancel(identity.operator());orders.flush();
        return OrderResponse.from(order);
    }
    private DeliveryOrder visible(Long id,Identity identity) {
        return (identity.operator()?orders.findById(id):orders.findByIdAndCustomerId(id,identity.id())).orElseThrow(ApiException::notFound);
    }
}
