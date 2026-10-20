package com.delivery.tracker.catalog;

import java.math.BigDecimal;
import java.util.List;
import com.delivery.tracker.auth.Identity;
import com.delivery.tracker.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {
    private final ProductRepository products;
    public ProductService(ProductRepository products) { this.products=products; }
    @Transactional(readOnly=true)
    public List<ProductView> list(Identity identity) {
        return (identity.operator()?products.findAllByOrderByIdAsc():products.findByAvailableTrueOrderByIdAsc()).stream().map(ProductView::from).toList();
    }
    @Transactional
    public ProductView create(String name,BigDecimal price,Boolean available) {
        return ProductView.from(products.saveAndFlush(new Product(name,price,available==null || available)));
    }
    @Transactional
    public ProductView edit(Long id,String name,BigDecimal price,boolean available) {
        var product=products.findLockedById(id).orElseThrow(ApiException::notFound);
        product.edit(name,price,available); products.flush();
        return ProductView.from(product);
    }
    public record ProductView(Long id,String name,BigDecimal price,boolean available) {
        public static ProductView from(Product p) { return new ProductView(p.getId(),p.getName(),p.getPrice(),p.isAvailable()); }
    }
}
