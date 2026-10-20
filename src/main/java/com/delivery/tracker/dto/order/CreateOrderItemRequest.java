package com.delivery.tracker.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateOrderItemRequest(@NotNull @Positive Long productId,@NotNull @Positive Integer quantity) {}
