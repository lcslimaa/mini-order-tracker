package com.delivery.tracker.dto.order;

import java.math.BigDecimal;

public record OrderItemResponse(Long productId,String name,BigDecimal unitPrice,int quantity,BigDecimal subtotal) {}
