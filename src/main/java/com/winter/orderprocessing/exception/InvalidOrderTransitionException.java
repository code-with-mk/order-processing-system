package com.winter.orderprocessing.exception;

import com.winter.orderprocessing.entity.OrderStatus;

public class InvalidOrderTransitionException extends RuntimeException {

    public InvalidOrderTransitionException(OrderStatus current, OrderStatus requested) {
        super("Cannot move an order from " + current + " to " + requested + ".");
    }

    public InvalidOrderTransitionException(OrderStatus current) {
        super("Only pending orders can be cancelled. Current status is " + current + ".");
    }
}
