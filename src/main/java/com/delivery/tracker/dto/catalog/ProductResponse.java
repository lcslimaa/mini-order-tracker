package com.delivery.tracker.dto.catalog;

import com.delivery.tracker.entity.Product;

import java.math.BigDecimal;

public record ProductResponse(Long id,String name,BigDecimal price,boolean available) {
    public static ProductResponse from(Product p) { return new ProductResponse(p.getId(),p.getName(),p.getPrice(),p.isAvailable()); }
}
