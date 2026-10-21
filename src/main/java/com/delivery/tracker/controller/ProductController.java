package com.delivery.tracker.controller;

import com.delivery.tracker.dto.catalog.CreateProductRequest;
import com.delivery.tracker.dto.catalog.ProductResponse;
import com.delivery.tracker.dto.catalog.UpdateProductRequest;
import com.delivery.tracker.security.Identity;
import com.delivery.tracker.service.ProductService;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:3000")
@RestController @RequestMapping("/api/products")
public class ProductController {
    private final ProductService service;
    public ProductController(ProductService service) { this.service=service; }
    @GetMapping public List<ProductResponse> list(@AuthenticationPrincipal Identity identity) { return service.list(identity); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody CreateProductRequest body) { return service.create(body.name(),body.price(),body.available()); }
    @PutMapping("/{id}")
    public ProductResponse edit(@PathVariable Long id,@Valid @RequestBody UpdateProductRequest body) { return service.edit(id,body.name(),body.price(),body.available()); }
}
