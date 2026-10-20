package com.delivery.tracker.service;

import com.delivery.tracker.dto.catalog.ProductResponse;
import com.delivery.tracker.entity.Product;
import com.delivery.tracker.exception.ApiException;
import com.delivery.tracker.repository.ProductRepository;
import com.delivery.tracker.security.Identity;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    public ProductService(ProductRepository products) { this.products=products; }
    @Transactional(readOnly=true)
    public List<ProductResponse> list(Identity identity) {
        return (identity.operator()?products.findAllByOrderByIdAsc():products.findByAvailableTrueOrderByIdAsc()).stream().map(ProductResponse::from).toList();
    }
    @Transactional
    public ProductResponse create(String name,BigDecimal price,Boolean available) {
        return ProductResponse.from(products.saveAndFlush(new Product(name,price,available==null || available)));
    }
    @Transactional
    public ProductResponse edit(Long id,String name,BigDecimal price,boolean available) {
        var product=products.findLockedById(id).orElseThrow(ApiException::notFound);
        product.edit(name,price,available); products.flush();
        return ProductResponse.from(product);
    }
}
