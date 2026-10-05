package com.winter.orderprocessing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotEmpty(message = "An order must contain at least one item.")
        List<@NotNull @Valid OrderItemRequest> items) {
}
