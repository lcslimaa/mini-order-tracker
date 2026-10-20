package com.delivery.tracker.orders;

import java.util.*;
import com.delivery.tracker.auth.*;
import com.delivery.tracker.catalog.*;
import com.delivery.tracker.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    public OrderService(OrderRepository orders,ProductRepository products,UserRepository users) { this.orders=orders;this.products=products;this.users=users; }
    @Transactional
    public OrderView create(Identity identity,OrderController.CreateOrder body) {
        var ids=body.items().stream().map(OrderController.CreateItem::productId).toList();
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
        return OrderView.from(orders.saveAndFlush(order));
    }
    @Transactional(readOnly=true)
    public List<OrderView> list(Identity identity) {
        return (identity.operator()?orders.findAllByOrderByIdAsc():orders.findByCustomerIdOrderByIdAsc(identity.id())).stream().map(OrderView::from).toList();
    }
    @Transactional(readOnly=true)
    public OrderView get(Long id,Identity identity) { return OrderView.from(visible(id,identity)); }
    private DeliveryOrder visible(Long id,Identity identity) {
        return (identity.operator()?orders.findById(id):orders.findByIdAndCustomerId(id,identity.id())).orElseThrow(ApiException::notFound);
    }
}
