package com.delivery.tracker.catalog;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity @Table(name="products")
public class Product {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=160) private String name;
    @Column(nullable=false,precision=12,scale=2) private BigDecimal price;
    @Column(nullable=false) private boolean available;
    @Version private long version;
    protected Product() {}
    public Product(String name,BigDecimal price,boolean available) { edit(name,price,available); }
    public void edit(String name,BigDecimal price,boolean available) { this.name=name.strip();this.price=price;this.available=available; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public boolean isAvailable() { return available; }
}
