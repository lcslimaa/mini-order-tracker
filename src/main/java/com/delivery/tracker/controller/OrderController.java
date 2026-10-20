package com.delivery.tracker.controller;

import com.delivery.tracker.dto.order.CreateOrderRequest;
import com.delivery.tracker.dto.order.OrderResponse;
import com.delivery.tracker.dto.order.UpdateOrderStatusRequest;
import com.delivery.tracker.security.Identity;
import com.delivery.tracker.service.OrderService;

import java.util.List;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/orders")
public class OrderController {
    private final OrderService service;
    public OrderController(OrderService service) { this.service=service; }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    OrderResponse create(@AuthenticationPrincipal Identity identity,@Valid @RequestBody CreateOrderRequest body) { return service.create(identity,body); }
    @GetMapping List<OrderResponse> list(@AuthenticationPrincipal Identity identity) { return service.list(identity); }
    @GetMapping("/{id}") OrderResponse get(@PathVariable Long id,@AuthenticationPrincipal Identity identity) { return service.get(id,identity); }

    @PatchMapping("/{id}/status")
    OrderResponse changeStatus(@PathVariable Long id,@AuthenticationPrincipal Identity identity,@Valid @RequestBody UpdateOrderStatusRequest body) {
        return service.changeStatus(id,identity,body.status());
    }
    @PostMapping("/{id}/cancel")
    OrderResponse cancel(@PathVariable Long id,@AuthenticationPrincipal Identity identity) { return service.cancel(id,identity); }
}
