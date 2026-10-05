package com.winter.orderprocessing.dto;

import com.winter.orderprocessing.entity.OrderItemEntity;

import java.math.BigDecimal;

public record OrderItemResponse(String productName, int quantity, BigDecimal unitPrice, BigDecimal lineTotal) {

    public static OrderItemResponse from(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
    }
}
