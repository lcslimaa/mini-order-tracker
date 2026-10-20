package com.delivery.tracker.dto.order;

import java.util.List;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record CreateOrderRequest(@NotBlank @Size(max=1000) String address,@NotEmpty List<@NotNull @Valid CreateOrderItemRequest> items) {}
