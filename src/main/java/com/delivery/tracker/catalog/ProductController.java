package com.delivery.tracker.catalog;

import java.math.BigDecimal;
import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.delivery.tracker.auth.Identity;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/products")
public class ProductController {
    public record CreateProduct(@NotBlank @Size(max=160) String name,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=2) BigDecimal price,Boolean available) {}
    public record EditProduct(@NotBlank @Size(max=160) String name,@NotNull @DecimalMin(value="0",inclusive=false) @Digits(integer=10,fraction=2) BigDecimal price,@NotNull Boolean available) {}
    private final ProductService service;
    public ProductController(ProductService service) { this.service=service; }
    @GetMapping public List<ProductService.ProductView> list(@AuthenticationPrincipal Identity identity) { return service.list(identity); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProductService.ProductView create(@Valid @RequestBody CreateProduct body) { return service.create(body.name(),body.price(),body.available()); }
    @PutMapping("/{id}")
    public ProductService.ProductView edit(@PathVariable Long id,@Valid @RequestBody EditProduct body) { return service.edit(id,body.name(),body.price(),body.available()); }
}
