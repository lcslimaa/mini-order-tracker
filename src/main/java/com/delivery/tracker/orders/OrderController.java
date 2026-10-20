package com.delivery.tracker.orders;

import java.util.List;
import com.delivery.tracker.auth.Identity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders")
public class OrderController {
    public record CreateItem(@NotNull @Positive Long productId,@NotNull @Positive Integer quantity) {}
    public record CreateOrder(@NotBlank @Size(max=1000) String address,@NotEmpty List<@NotNull @Valid CreateItem> items) {}
    private final OrderService service;
    public OrderController(OrderService service) { this.service=service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    OrderView create(@AuthenticationPrincipal Identity identity,@Valid @RequestBody CreateOrder body) { return service.create(identity,body); }
    @GetMapping List<OrderView> list(@AuthenticationPrincipal Identity identity) { return service.list(identity); }
    @GetMapping("/{id}") OrderView get(@PathVariable Long id,@AuthenticationPrincipal Identity identity) { return service.get(id,identity); }
}
