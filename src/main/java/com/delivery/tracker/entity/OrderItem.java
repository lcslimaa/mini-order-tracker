package com.delivery.tracker.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="order_id") private DeliveryOrder order;
    @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="product_id") private Product product;
    @Column(nullable=false,length=160) private String name;
    @Column(name="unit_price",nullable=false,precision=12,scale=2) private BigDecimal unitPrice;
    @Column(nullable=false) private int quantity;
    @Column(nullable=false,precision=24,scale=2) private BigDecimal subtotal;
    protected OrderItem() {}
    OrderItem(DeliveryOrder order,Product product,int quantity) {
        this.order=order;this.product=product;this.name=product.getName();this.unitPrice=product.getPrice();
        this.quantity=quantity;this.subtotal=unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
    public Long getProductId() { return product.getId(); }
    public String getName() { return name; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public int getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
}
