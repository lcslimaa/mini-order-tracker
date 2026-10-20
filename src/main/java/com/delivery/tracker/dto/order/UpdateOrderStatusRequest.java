package com.delivery.tracker.dto.order;

import com.delivery.tracker.enums.OrderStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(@NotNull OrderStatus status) {}
