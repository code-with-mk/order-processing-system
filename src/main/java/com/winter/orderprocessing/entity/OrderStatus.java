package com.winter.orderprocessing.entity;

public enum OrderStatus {
    PENDING,
    PROCESSING,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PROCESSING -> target == SHIPPED;
            case SHIPPED -> target == DELIVERED;
            case PENDING, DELIVERED, CANCELLED -> false;
        };
    }
}
