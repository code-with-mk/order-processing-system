package com.winter.orderprocessing.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank(message = "Product name is required.")
        @Size(max = 120, message = "Product name must be at most 120 characters.")
        String productName,
        @Positive(message = "Quantity must be greater than zero.")
        int quantity,
        @NotNull(message = "Unit price is required.")
        @DecimalMin(value = "0.00", message = "Unit price cannot be negative.")
        @Digits(integer = 10, fraction = 2, message = "Unit price must have at most two decimal places.")
        BigDecimal unitPrice) {
}
