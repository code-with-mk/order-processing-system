package com.winter.orderprocessing.dto;

import com.winter.orderprocessing.entity.OrderEntity;
import com.winter.orderprocessing.entity.OrderItemEntity;
import com.winter.orderprocessing.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, Instant createdAt, OrderStatus status,
                            List<OrderItemResponse> items, BigDecimal total) {

    public static OrderResponse from(OrderEntity order) {
        List<OrderItemEntity> orderItems = order.getItems();
        List<OrderItemResponse> responseItems = orderItems.stream().map(OrderItemResponse::from).toList();
        BigDecimal total = responseItems.stream()
                .map(OrderItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new OrderResponse(order.getId(), order.getCreatedAt(), order.getStatus(), responseItems, total);
    }
}
