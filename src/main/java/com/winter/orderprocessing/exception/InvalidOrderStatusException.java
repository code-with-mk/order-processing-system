package com.winter.orderprocessing.exception;

public class InvalidOrderStatusException extends RuntimeException {

    public InvalidOrderStatusException(String status) {
        super("Unknown order status: " + status + ".");
    }
}
