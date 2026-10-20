package com.delivery.tracker.orders;

import com.delivery.tracker.auth.AppUser;
import com.delivery.tracker.catalog.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Entity @Table(name="orders")
public class DeliveryOrder {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="customer_id") private AppUser customer;
    @Column(nullable=false,length=1000) private String address;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=32) private OrderStatus status;
    @Column(nullable=false,precision=24,scale=2) private BigDecimal total;
    @Column(name="created_at",nullable=false) private Instant createdAt;
    @Version private long version;
    @OneToMany(mappedBy="order",cascade=CascadeType.ALL) @OrderBy("id ASC") private List<OrderItem> items=new ArrayList<>();
    protected DeliveryOrder() {}
    public DeliveryOrder(AppUser customer,String address) {
        this.customer=customer;this.address=address.strip();this.status=OrderStatus.RECEBIDO;
        this.createdAt=Instant.now();this.total=BigDecimal.ZERO;
    }
    public void addItem(Product product,int quantity) {
        var item=new OrderItem(this,product,quantity);items.add(item);total=total.add(item.getSubtotal());
    }
    public Long getId() { return id; }
    public AppUser getCustomer() { return customer; }
    public String getAddress() { return address; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return Collections.unmodifiableList(items); }
}
